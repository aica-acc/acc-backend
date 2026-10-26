package com.assistant.acc.controller.auth;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.assistant.acc.dto.auth.AuthDtos.LoginRequest;
import com.assistant.acc.dto.auth.AuthDtos.SignupRequest;
import com.assistant.acc.security.CurrentMember;
import com.assistant.acc.service.auth.AuthService;

import lombok.RequiredArgsConstructor;

/** 회원가입 / 로그인 / test 로그인 / 내 정보 (JWT) */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** 로그인 화면이 test 로그인 버튼을 보여줄지 결정할 때 사용 (인증 불필요) */
    @GetMapping("/config")
    public Map<String, Object> config() {
        return Map.of("testLoginEnabled", authService.isTestLoginEnabled());
    }

    @PostMapping("/signup")
    public Map<String, Object> signup(@RequestBody SignupRequest req) {
        return authService.signup(req);
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginRequest req) {
        return authService.login(req);
    }

    @PostMapping("/test-login")
    public Map<String, Object> testLogin() {
        return authService.testLogin();
    }

    @GetMapping("/me")
    public Map<String, Object> me() {
        return authService.me(CurrentMember.no());
    }
}
