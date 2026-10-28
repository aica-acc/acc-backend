package com.assistant.acc.controller.auth;

import com.assistant.acc.dto.auth.AuthDtos.PasswordRequest;
import com.assistant.acc.dto.auth.AuthDtos.ProfileRequest;
import org.springframework.web.bind.annotation.PatchMapping;
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
import com.assistant.acc.service.auth.SocialLoginService;
import org.springframework.web.bind.annotation.PathVariable;

import lombok.RequiredArgsConstructor;

/** 회원가입 / 로그인 / test 로그인 / 내 정보 (JWT) */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SocialLoginService socialLoginService;

    /** 화면이 체험하기·소셜 로그인 버튼을 보여줄지 결정할 때 사용 (인증 불필요). social: {제공자: 공개 client id} */
    @GetMapping("/config")
    public Map<String, Object> config() {
        return Map.of("testLoginEnabled", authService.isTestLoginEnabled(),
                "social", socialLoginService.publicClientIds());
    }

    /** 소셜 로그인: 제공자 로그인 창에서 돌아온 code 로 로그인(처음이면 가입). body: {code, redirectUri} */
    @PostMapping("/oauth/{provider}")
    public Map<String, Object> oauth(@PathVariable String provider, @RequestBody Map<String, String> body) {
        return socialLoginService.login(provider, body.get("code"), body.get("redirectUri"));
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

    @PatchMapping("/me")
    public Map<String, Object> updateMe(@RequestBody ProfileRequest req) {
        return authService.updateProfile(CurrentMember.no(), req);
    }

    @PostMapping("/password")
    public Map<String, Object> changePassword(@RequestBody PasswordRequest req) {
        authService.changePassword(CurrentMember.no(), req);
        return Map.of("changed", true);
    }
}
