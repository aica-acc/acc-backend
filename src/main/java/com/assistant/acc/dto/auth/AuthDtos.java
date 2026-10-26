package com.assistant.acc.dto.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

/**
 * 프론트 axios 인터셉터가 모든 JSON 요청에 m_no 를 덧붙이므로, 알 수 없는 필드는 무시한다
 * (이 프로젝트의 ObjectMapper 는 알 수 없는 필드를 거절하도록 설정되어 있다).
 */
public final class AuthDtos {

    private AuthDtos() {
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SignupRequest {
        private String email;
        private String password;
        private String name;
        private String phone;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LoginRequest {
        private String email;
        private String password;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RoleRequest {
        private String role;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StateRequest {
        private String state;
    }
}
