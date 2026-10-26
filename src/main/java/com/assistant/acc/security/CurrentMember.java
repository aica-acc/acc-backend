package com.assistant.acc.security;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

/** 현재 요청의 로그인 회원 (JwtAuthFilter 가 심어 둔 값). 서비스 계층에서 하드코딩된 회원번호 대신 사용한다. */
public final class CurrentMember {

    private CurrentMember() {
    }

    public static String no() {
        Object v = request().getAttribute("m_no");
        if (v == null) {
            throw new IllegalStateException("로그인이 필요합니다.");
        }
        return v.toString();
    }

    public static boolean isAdmin() {
        return "ADMIN".equals(request().getAttribute("role"));
    }

    private static HttpServletRequest request() {
        return ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
    }
}
