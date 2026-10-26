package com.assistant.acc.service.auth;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.assistant.acc.domain.member.Member;
import com.assistant.acc.dto.auth.AuthDtos.LoginRequest;
import com.assistant.acc.dto.auth.AuthDtos.SignupRequest;
import com.assistant.acc.mapper.member.MemberMapper;
import com.assistant.acc.security.JwtTokenProvider;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final MemberMapper memberMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokens;

    @Value("${auth.test-login.enabled:true}")
    private boolean testLoginEnabled;

    @Value("${auth.test.email:test@acc.local}")
    private String testEmail;

    public boolean isTestLoginEnabled() {
        return testLoginEnabled;
    }

    public Map<String, Object> signup(SignupRequest req) {
        String email = req.getEmail() == null ? "" : req.getEmail().trim().toLowerCase();
        String name = req.getName() == null ? "" : req.getName().trim();
        if (!EMAIL.matcher(email).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이메일 형식이 올바르지 않습니다.");
        }
        if (req.getPassword() == null || req.getPassword().length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "비밀번호는 8자 이상이어야 합니다.");
        }
        if (name.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이름을 입력해주세요.");
        }
        if (memberMapper.findByEmail(email) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 가입된 이메일입니다.");
        }
        Member m = createMember(email, name, req.getPhone(), req.getPassword(), Member.ROLE_USER, null);
        return session(m);
    }

    public Map<String, Object> login(LoginRequest req) {
        String email = req.getEmail() == null ? "" : req.getEmail().trim().toLowerCase();
        Member m = memberMapper.findByEmail(email);
        // 계정 존재 여부를 알려주지 않도록 같은 메시지를 사용한다
        if (m == null || m.getPasswordHash() == null || req.getPassword() == null
                || !passwordEncoder.matches(req.getPassword(), m.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다.");
        }
        if (!m.isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "사용이 정지된 계정입니다.");
        }
        return session(m);
    }

    /** 비밀번호 없이 test 계정 토큰 발급 (auth.test-login.enabled=false 로 막을 수 있다) */
    public Map<String, Object> testLogin() {
        if (!testLoginEnabled) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "테스트 로그인이 비활성화되어 있습니다.");
        }
        Member m = memberMapper.findByEmail(testEmail.toLowerCase());
        if (m == null || !m.isActive()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "테스트 계정이 준비되지 않았습니다.");
        }
        return session(m);
    }

    public Map<String, Object> me(String mNo) {
        Member m = memberMapper.findByNo(mNo);
        if (m == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "회원 정보를 찾을 수 없습니다.");
        }
        return view(m);
    }

    /** 회원번호는 M + 6자리 일련번호. 동시 가입 충돌은 PK 로 막고 재시도한다. */
    public synchronized Member createMember(String email, String name, String phone, String rawPassword,
            String role, String fixedNo) {
        for (int attempt = 0; attempt < 5; attempt++) {
            Member m = new Member();
            m.setMNo(fixedNo != null ? fixedNo : nextNo());
            m.setEmail(email);
            m.setName(name);
            m.setPhone(phone == null ? "" : phone.trim());
            m.setPasswordHash(rawPassword == null ? null : passwordEncoder.encode(rawPassword));
            m.setRole(role);
            m.setState(Member.STATE_ACTIVE);
            try {
                memberMapper.insert(m);
                return memberMapper.findByNo(m.getMNo());
            } catch (org.springframework.dao.DuplicateKeyException e) {
                if (memberMapper.findByEmail(email) != null || fixedNo != null) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 존재하는 계정입니다.");
                }
            }
        }
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "회원번호를 발급하지 못했습니다.");
    }

    private String nextNo() {
        String max = memberMapper.selectMaxNo();
        int n = 0;
        if (max != null && max.length() > 1) {
            try {
                n = Integer.parseInt(max.substring(1));
            } catch (NumberFormatException ignore) {
                n = 0;
            }
        }
        return String.format("M%06d", n + 1);
    }

    private Map<String, Object> session(Member m) {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("token", tokens.create(m));
        res.put("member", view(m));
        return res;
    }

    public static Map<String, Object> view(Member m) {
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("mNo", m.getMNo());
        v.put("email", m.getEmail());
        v.put("name", m.getName());
        v.put("phone", m.getPhone());
        v.put("role", m.getRole());
        v.put("state", m.getState());
        v.put("createAt", m.getCreateAt() == null ? null : m.getCreateAt().toString());
        return v;
    }
}
