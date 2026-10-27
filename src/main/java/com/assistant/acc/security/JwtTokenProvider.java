package com.assistant.acc.security;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.assistant.acc.domain.member.Member;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

/**
 * JWT 발급/검증. 비밀키는 환경변수 AUTH_JWT_SECRET (32바이트 이상) 로 주입한다.
 * 비어 있으면 기동할 때마다 랜덤 키를 만들므로, 재시작 시 기존 토큰이 모두 무효화된다.
 * 토큰은 브라우저에서 누구나 열어 볼 수 있으므로 회원번호는 암호화해 담고, 이메일·이름·권한은 담지 않는다
 * (권한·상태는 매 요청 DB 에서 다시 읽는다).
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long expireMillis;
    private final UrlCipher cipher;

    public JwtTokenProvider(
            @Value("${auth.jwt.secret:}") String secret,
            @Value("${auth.jwt.expire-hours:12}") long expireHours,
            UrlCipher cipher) {
        this.cipher = cipher;
        byte[] bytes;
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            if (secret != null && !secret.isBlank()) {
                log.warn("auth.jwt.secret 이 32바이트 미만입니다. 랜덤 키를 사용합니다.");
            } else {
                log.warn("auth.jwt.secret(AUTH_JWT_SECRET) 이 비어 있어 랜덤 키를 사용합니다. 재시작하면 로그인이 풀립니다.");
            }
            bytes = new byte[48];
            new SecureRandom().nextBytes(bytes);
        } else {
            bytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.expireMillis = expireHours * 3600_000L;
    }

    public String create(Member m) {
        Date now = new Date();
        return Jwts.builder()
                .subject(cipher.encrypt("m:" + m.getMNo()))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expireMillis))
                .signWith(key)
                .compact();
    }

    /** 토큰의 회원번호 (암호화된 subject 를 푼다). 형식이 맞지 않으면 null */
    public String memberNo(Claims claims) {
        String plain = claims.getSubject() == null ? null : cipher.decrypt(claims.getSubject());
        return plain != null && plain.startsWith("m:") ? plain.substring(2) : null;
    }

    /** 유효하면 claims, 아니면 null */
    public Claims parse(String token) {
        try {
            return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }
}
