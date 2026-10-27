package com.assistant.acc.controller.proxy;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * AI 서버 정적 파일(/static/**) 중계.
 * 브라우저는 백엔드만 바라보고, AI 서버는 외부에 노출하지 않는다.
 * <img src> 로 직접 불러오므로 토큰 없이 접근 가능 (WebConfig 의 PUBLIC_PATHS).
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class AiStaticProxyController {

    // AI 응답 헤더 중 브라우저에 그대로 넘길 것
    private static final List<String> PASS_HEADERS = List.of(
            HttpHeaders.CONTENT_TYPE,
            HttpHeaders.CONTENT_LENGTH,
            HttpHeaders.LAST_MODIFIED,
            HttpHeaders.ETAG,
            HttpHeaders.CACHE_CONTROL);

    private final RestTemplate restTemplate;

    @Value("${python.api.url:http://localhost:5000}")
    private String pythonApiUrl;

    @GetMapping("/static/**")
    public void proxy(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // 인코딩된 원래 경로를 그대로 전달한다 (한글 파일명 이중 인코딩 방지)
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (path.contains("..") || path.contains("%2e") || path.contains("%2E")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        URI target = URI.create(pythonApiUrl.replaceAll("/+$", "") + path);

        try {
            restTemplate.execute(target, HttpMethod.GET, null, upstream -> {
                response.setStatus(upstream.getStatusCode().value());
                HttpHeaders headers = upstream.getHeaders();
                for (String name : PASS_HEADERS) {
                    String value = headers.getFirst(name);
                    if (value != null) {
                        response.setHeader(name, value);
                    }
                }
                StreamUtils.copy(upstream.getBody(), response.getOutputStream());
                return null;
            });
        } catch (HttpStatusCodeException e) {
            response.sendError(e.getStatusCode().value());
        } catch (Exception e) {
            log.warn("AI 정적 파일 중계 실패: {} ({})", target, e.getMessage());
            response.sendError(HttpStatus.BAD_GATEWAY.value());
        }
    }
}
