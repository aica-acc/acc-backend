package com.assistant.acc.config.web;

import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import com.assistant.acc.security.DemoBlockedException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 데모(test) 계정은 유료 AI 생성과 공용 데모 데이터를 바꾸는 요청을 하지 못하게 막는다.
 * 생성 작업(/api/create-jobs, /api/editor/build-jobs)과 리포트는 각 컨트롤러가 "미리 만든 결과만" 쓰도록 처리하므로 여기서 막지 않는다.
 */
public class DemoGuardInterceptor implements HandlerInterceptor {

    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    /** 데모에서 막는 요청 (메서드, 경로) */
    private static final String[][] BLOCKED = {
            {"POST", "/api/project/analyze/**"},      // 기획서·트렌드 분석 (유료)
            {"POST", "/api/posters/*/regenerate"},    // 포스터 다시 만들기 (유료)
            {"POST", "/api/editor/ai-render"},        // AI 색상 추천 (유료)
            {"POST", "/api/editor/save-image"},       // 데모 프로젝트에 이미지 저장
            {"DELETE", "/api/project/**"},            // 데모 프로젝트 삭제
            {"PUT", "/api/progress/**"},              // 데모 프로젝트 진행 상태·편집 내용 저장 (모두가 같이 보는 데이터)
    };

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!"TEST".equals(request.getAttribute("role"))) {
            return true;
        }
        String path = request.getRequestURI();
        for (String[] rule : BLOCKED) {
            if (rule[0].equalsIgnoreCase(request.getMethod()) && MATCHER.match(rule[1], path)) {
                throw new DemoBlockedException();
            }
        }
        return true;
    }
}
