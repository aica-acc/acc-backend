package com.assistant.acc.security;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 브라우저에 내보내는 내부 값(파일 경로, 회원번호)을 알아볼 수 없는 토큰으로 바꾼다.
 *
 * AES-GCM 으로 암호화하되 IV 를 평문의 HMAC 으로 만든다(결정적 암호화).
 * 같은 값은 항상 같은 토큰이 되어 브라우저 캐시와 "같은 파일인지" 비교가 그대로 동작하고,
 * 토큰을 바꾸거나 지어내면 복호화가 실패한다. 키는 url-secret → JWT 비밀키 순으로 정한다.
 */
@Slf4j
@Component
public class UrlCipher {

    private static final int IV_LEN = 12;
    private static final Base64.Encoder ENC = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DEC = Base64.getUrlDecoder();

    private final SecretKeySpec encKey;
    private final SecretKeySpec macKey;

    public UrlCipher(@Value("${auth.url-secret:}") String urlSecret,
            @Value("${auth.jwt.secret:}") String jwtSecret) {
        String secret = !urlSecret.isBlank() ? urlSecret : jwtSecret;
        byte[] seed;
        if (secret.isBlank()) {
            log.warn("auth.url-secret / auth.jwt.secret 이 비어 있어 랜덤 키를 사용합니다. 재시작하면 파일 주소가 바뀝니다.");
            seed = new byte[32];
            new SecureRandom().nextBytes(seed);
        } else {
            seed = secret.getBytes(StandardCharsets.UTF_8);
        }
        this.encKey = new SecretKeySpec(Arrays.copyOf(sha256("enc", seed), 16), "AES");
        this.macKey = new SecretKeySpec(sha256("mac", seed), "HmacSHA256");
    }

    public String encrypt(String plain) {
        try {
            byte[] data = plain.getBytes(StandardCharsets.UTF_8);
            byte[] iv = Arrays.copyOf(hmac(data), IV_LEN);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, encKey, new GCMParameterSpec(128, iv));
            byte[] ct = c.doFinal(data);
            return ENC.encodeToString(ByteBuffer.allocate(IV_LEN + ct.length).put(iv).put(ct).array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("토큰 생성 실패", e);
        }
    }

    /** 잘못되었거나 위조된 토큰이면 null */
    public String decrypt(String token) {
        try {
            byte[] raw = DEC.decode(token);
            if (raw.length <= IV_LEN + 16) {
                return null;
            }
            byte[] iv = Arrays.copyOf(raw, IV_LEN);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, encKey, new GCMParameterSpec(128, iv));
            byte[] data = c.doFinal(raw, IV_LEN, raw.length - IV_LEN);
            // IV 가 평문의 HMAC 과 맞아야 이 서버가 만든 토큰이다
            if (!MessageDigest.isEqual(iv, Arrays.copyOf(hmac(data), IV_LEN))) {
                return null;
            }
            return new String(data, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException | GeneralSecurityException e) {
            return null;
        }
    }

    private byte[] hmac(byte[] data) throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(macKey);
        return mac.doFinal(data);
    }

    private static byte[] sha256(String label, byte[] seed) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(label.getBytes(StandardCharsets.UTF_8));
            return md.digest(seed);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
