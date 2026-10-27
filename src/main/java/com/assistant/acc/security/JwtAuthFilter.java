package com.assistant.acc.security;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.assistant.acc.domain.member.Member;
import com.assistant.acc.mapper.member.MemberMapper;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Authorization: Bearer 토큰을 검증하고 회원 정보를 요청에 심는다.
 *  - request attribute "m_no" / "role" : 기존 컨트롤러가 request.getAttribute("m_no") 로 읽는다.
 *  - 권한/상태는 매 요청 DB 에서 다시 확인하므로, 관리자가 권한을 바꾸거나 계정을 정지하면 즉시 반영된다.
 * 스프링 빈으로 등록하지 않고 SecurityFilterChain 에서만 생성한다 (서블릿 필터 중복 등록 방지).
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokens;
    private final MemberMapper members;

    public JwtAuthFilter(JwtTokenProvider tokens, MemberMapper members) {
        this.tokens = tokens;
        this.members = members;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            Claims claims = tokens.parse(header.substring(7).trim());
            if (claims != null) {
                String mNo = tokens.memberNo(claims);
                Member m = mNo == null ? null : members.findByNo(mNo);
                if (m != null && m.isActive()) {
                    request.setAttribute("m_no", m.getMNo());
                    request.setAttribute("role", m.getRole());
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(
                                    m.getMNo(), null,
                                    List.of(new SimpleGrantedAuthority("ROLE_" + m.getRole()))));
                }
            }
        }
        chain.doFilter(request, response);
    }
}
