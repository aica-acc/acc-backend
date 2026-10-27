package com.assistant.acc.security;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;

/**
 * 프로젝트 소유권 검사.
 * 요청에 담긴 프로젝트 번호(pNo)를 그대로 믿으면 다른 회원의 프로젝트에 읽고 쓸 수 있으므로,
 * pNo 를 받는 API 는 처리 전에 check(pNo) 로 로그인한 회원의 프로젝트인지 확인한다. (관리자는 통과)
 */
@Component
@RequiredArgsConstructor
public class ProjectAccess {

    private final JdbcTemplate jdbcTemplate;

    /** 로그인한 회원의 프로젝트가 아니면 404(없음) / 403(남의 것)을 던진다. */
    public void check(Integer pNo) {
        if (pNo == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "프로젝트 번호가 없습니다.");
        }
        String owner = jdbcTemplate.query(
                "SELECT m_no FROM project WHERE p_no = ?",
                rs -> rs.next() ? rs.getString(1) : null,
                pNo);
        if (owner == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "프로젝트를 찾을 수 없습니다.");
        }
        if (!owner.equals(CurrentMember.no()) && !CurrentMember.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "다른 회원의 프로젝트입니다.");
        }
    }
}
