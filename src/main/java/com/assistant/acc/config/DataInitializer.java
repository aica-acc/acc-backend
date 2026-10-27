package com.assistant.acc.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.assistant.acc.domain.member.Member;
import com.assistant.acc.mapper.member.MemberMapper;
import com.assistant.acc.service.auth.AuthService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 기동 시 test / admin 계정을 보장한다.
 *  - test  : 비밀번호 없이 "test 계정으로 로그인" 버튼으로만 접근 (회원번호 M000001 — 기존 코드/폴더 규칙과 호환)
 *  - admin : AUTH_ADMIN_EMAIL / AUTH_ADMIN_PASSWORD 가 설정된 경우에만 생성. 이미 있으면 ADMIN 권한만 보장한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final MemberMapper memberMapper;
    private final AuthService authService;

    @Value("${auth.test.email:test@acc.local}")
    private String testEmail;

    @Value("${auth.test.name:테스트 계정}")
    private String testName;

    @Value("${auth.admin.email:admin@acc.local}")
    private String adminEmail;

    @Value("${auth.admin.password:}")
    private String adminPassword;

    @Override
    public void run(ApplicationArguments args) {
        try {
            ensureTest();
            ensureAdmin();
        } catch (Exception e) {
            // 스키마(03_auth.sql)가 적용되지 않은 DB 등: 서버는 띄우고 원인을 로그로 남긴다
            log.error("계정 초기화 실패 - db/03_auth.sql 이 적용되었는지 확인하세요: {}", e.getMessage());
        }
    }

    private void ensureTest() {
        String email = testEmail.toLowerCase();
        Member m = memberMapper.findByEmail(email);
        if (m == null) {
            // M000001 이 비어 있으면 그 번호를 쓰고, 아니면 일련번호를 발급받는다
            String fixed = memberMapper.findByNo("M000001") == null ? "M000001" : null;
            m = authService.createMember(email, testName, "", null, Member.ROLE_TEST, fixed);
            log.info("test 계정 생성: {} ({})", m.getEmail(), m.getMNo());
        } else if (!Member.ROLE_TEST.equals(m.getRole())) {
            memberMapper.updateRole(m.getMNo(), Member.ROLE_TEST);
        }
    }

    private void ensureAdmin() {
        String email = adminEmail.toLowerCase();
        Member m = memberMapper.findByEmail(email);
        if (m == null) {
            if (adminPassword == null || adminPassword.length() < 8) {
                log.warn("AUTH_ADMIN_PASSWORD(8자 이상)가 없어 관리자 계정을 만들지 않았습니다.");
                return;
            }
            m = authService.createMember(email, "관리자", "", adminPassword, Member.ROLE_ADMIN, null);
            log.info("admin 계정 생성: {} ({})", m.getEmail(), m.getMNo());
        } else if (!Member.ROLE_ADMIN.equals(m.getRole())) {
            memberMapper.updateRole(m.getMNo(), Member.ROLE_ADMIN);
        }
    }
}
