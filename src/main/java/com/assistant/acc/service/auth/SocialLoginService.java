package com.assistant.acc.service.auth;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import com.assistant.acc.domain.member.Member;
import com.assistant.acc.mapper.member.MemberMapper;
import com.assistant.acc.service.credit.CreditService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 카카오 / 구글 로그인 (OAuth 2.0 인가 코드 방식).
 * 화면이 제공자 로그인 창으로 보낸 뒤 돌아온 code 를 받아 토큰 → 회원 정보를 조회하고, ACC 회원으로 로그인시킨다.
 *  - 처음이면 일반 회원(USER)으로 가입 + 가입 기념 크레딧
 *  - 같은 이메일의 기존 회원은 제공자가 "인증된 이메일"이라고 할 때만 연결한다 (남의 이메일로 계정을 가져가지 못하게)
 *  - 이메일을 주지 않으면 내부용 대체 이메일로 가입한다 (member.m_email 이 필수라서)
 * 키가 없는 제공자는 쓰지 않는다 (화면에 버튼이 나오지 않음).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SocialLoginService {

    public static final String KAKAO = "KAKAO";
    public static final String GOOGLE = "GOOGLE";

    private final MemberMapper memberMapper;
    private final AuthService authService;
    private final CreditService creditService;
    private final RestTemplate restTemplate;

    @Value("${oauth.kakao.client-id:}")
    private String kakaoClientId;
    @Value("${oauth.kakao.client-secret:}")
    private String kakaoClientSecret;
    @Value("${oauth.google.client-id:}")
    private String googleClientId;
    @Value("${oauth.google.client-secret:}")
    private String googleClientSecret;

    /** 화면이 로그인 창 주소를 만들 때 쓰는 공개 client id (설정된 제공자만) */
    public Map<String, String> publicClientIds() {
        Map<String, String> out = new LinkedHashMap<>();
        if (!kakaoClientId.isBlank()) {
            out.put("kakao", kakaoClientId);
        }
        if (!googleClientId.isBlank() && !googleClientSecret.isBlank()) {
            out.put("google", googleClientId);
        }
        return out;
    }

    public Map<String, Object> login(String provider, String code, String redirectUri) {
        String p = provider == null ? "" : provider.toUpperCase(Locale.ROOT);
        if (code == null || code.isBlank() || redirectUri == null || redirectUri.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "로그인 정보가 올바르지 않습니다.");
        }
        Profile profile = switch (p) {
            case KAKAO -> kakao(code, redirectUri);
            case GOOGLE -> google(code, redirectUri);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 로그인 방식입니다.");
        };
        Member m = findOrCreate(p, profile);
        if (!m.isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "사용이 정지된 계정입니다.");
        }
        return authService.socialSession(m);
    }

    private Member findOrCreate(String provider, Profile profile) {
        Member linked = memberMapper.findBySocial(provider, profile.id());
        if (linked != null) {
            return linked;
        }
        String email = profile.email() == null ? null : profile.email().trim().toLowerCase(Locale.ROOT);
        if (email != null && profile.emailVerified()) {
            Member existing = memberMapper.findByEmail(email);
            if (existing != null) {
                if (Member.ROLE_TEST.equals(existing.getRole())) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "이 계정으로는 로그인할 수 없습니다.");
                }
                memberMapper.linkSocial(existing.getMNo(), provider, profile.id());
                log.info("[social] 기존 회원에 연결 mNo={}, provider={}", existing.getMNo(), provider);
                return memberMapper.findByNo(existing.getMNo());
            }
        }
        // 이메일이 없거나(동의 안 함), 인증되지 않았는데 이미 쓰이는 주소면 내부용 대체 이메일로 가입
        if (email == null || email.length() > 50 || memberMapper.findByEmail(email) != null) {
            email = provider.toLowerCase(Locale.ROOT) + "_" + profile.id() + "@social.acc";
            if (email.length() > 50) {
                email = email.substring(email.length() - 50);
            }
        }
        String name = profile.name() == null || profile.name().isBlank() ? "회원" : profile.name().trim();
        if (name.length() > 50) {
            name = name.substring(0, 50);
        }
        Member m = authService.createMember(email, name, "", null, Member.ROLE_USER, null);
        memberMapper.linkSocial(m.getMNo(), provider, profile.id());
        creditService.grantSignupBonus(m);
        log.info("[social] 가입 mNo={}, provider={}", m.getMNo(), provider);
        return memberMapper.findByNo(m.getMNo());
    }

    // ---------------- 제공자별 ----------------

    @SuppressWarnings("unchecked")
    private Profile kakao(String code, String redirectUri) {
        if (kakaoClientId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "카카오 로그인이 설정되어 있지 않습니다.");
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", kakaoClientId);
        form.add("redirect_uri", redirectUri);
        form.add("code", code);
        if (!kakaoClientSecret.isBlank()) {
            form.add("client_secret", kakaoClientSecret);
        }
        String accessToken = token("https://kauth.kakao.com/oauth/token", form);
        Map<String, Object> me = userInfo("https://kapi.kakao.com/v2/user/me", accessToken);
        Map<String, Object> account = (Map<String, Object>) me.getOrDefault("kakao_account", Map.of());
        Map<String, Object> prof = (Map<String, Object>) account.getOrDefault("profile", Map.of());
        return new Profile(
                String.valueOf(me.get("id")),
                (String) account.get("email"),
                Boolean.TRUE.equals(account.get("is_email_valid")) && Boolean.TRUE.equals(account.get("is_email_verified")),
                (String) prof.get("nickname"));
    }

    private Profile google(String code, String redirectUri) {
        if (googleClientId.isBlank() || googleClientSecret.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "구글 로그인이 설정되어 있지 않습니다.");
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", googleClientId);
        form.add("client_secret", googleClientSecret);
        form.add("redirect_uri", redirectUri);
        form.add("code", code);
        String accessToken = token("https://oauth2.googleapis.com/token", form);
        Map<String, Object> me = userInfo("https://openidconnect.googleapis.com/v1/userinfo", accessToken);
        return new Profile(
                String.valueOf(me.get("sub")),
                (String) me.get("email"),
                Boolean.TRUE.equals(me.get("email_verified")),
                (String) me.get("name"));
    }

    @SuppressWarnings("unchecked")
    private String token(String url, MultiValueMap<String, String> form) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        try {
            Map<String, Object> res = restTemplate.postForObject(url, new HttpEntity<>(form, headers), Map.class);
            Object token = res == null ? null : res.get("access_token");
            if (token == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "소셜 로그인에 실패했습니다. 다시 시도해 주세요.");
            }
            return token.toString();
        } catch (RestClientException e) {
            log.warn("[social] 토큰 발급 실패 {} ({})", url, e.getMessage());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "소셜 로그인에 실패했습니다. 다시 시도해 주세요.");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> userInfo(String url, String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        try {
            Map<String, Object> res = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Map.class).getBody();
            if (res == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "소셜 계정 정보를 가져오지 못했습니다.");
            }
            return res;
        } catch (RestClientException e) {
            log.warn("[social] 회원 정보 조회 실패 {} ({})", url, e.getMessage());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "소셜 계정 정보를 가져오지 못했습니다.");
        }
    }

    private record Profile(String id, String email, boolean emailVerified, String name) {
    }
}
