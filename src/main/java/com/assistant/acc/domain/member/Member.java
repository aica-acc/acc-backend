package com.assistant.acc.domain.member;

import java.time.LocalDate;

import lombok.Data;

/**
 * member 테이블 (인증 정보 포함). 생성된 홍보물 데이터는 다루지 않는다.
 */
@Data
public class Member {
    private String mNo;          // M000001 형식
    private String email;
    private String name;
    private String phone;
    private String passwordHash; // BCrypt
    private String role;         // USER / TEST / ADMIN
    private String state;        // ACTIVE / SUSPENDED / DELETED
    private LocalDate createAt;
    private LocalDate deleteAt;

    public static final String ROLE_USER = "USER";
    public static final String ROLE_TEST = "TEST";
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String STATE_ACTIVE = "ACTIVE";

    public boolean isActive() {
        return state == null || STATE_ACTIVE.equalsIgnoreCase(state);
    }
}
