package com.assistant.acc.config.web;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import com.assistant.acc.service.file.PublicFileUrls;

import lombok.RequiredArgsConstructor;

/**
 * 요청 본문의 /files/토큰 을 서버가 쓰던 경로로 되돌린다 (PublicFileUrls.toInternal).
 * 화면이 받은 주소를 그대로 돌려보내도(베이스 선택, 편집 저장, 진행 상태) 기존 생성 로직은 바뀐 것 없이 동작한다.
 */
@ControllerAdvice
@RequiredArgsConstructor
public class PublicPathRequestAdvice extends RequestBodyAdviceAdapter {

    private final PublicFileUrls urls;

    @Override
    public boolean supports(MethodParameter methodParameter, Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage, MethodParameter parameter, Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType) throws IOException {
        byte[] raw = inputMessage.getBody().readAllBytes();
        String text = new String(raw, StandardCharsets.UTF_8);
        if (!text.contains(PublicFileUrls.PREFIX)) {
            return replay(inputMessage.getHeaders(), raw);
        }
        byte[] rewritten = urls.toInternal(text).getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.putAll(inputMessage.getHeaders());
        headers.setContentLength(rewritten.length);
        return replay(headers, rewritten);
    }

    private static HttpInputMessage replay(HttpHeaders headers, byte[] body) {
        return new HttpInputMessage() {
            @Override
            public InputStream getBody() {
                return new ByteArrayInputStream(body);
            }

            @Override
            public HttpHeaders getHeaders() {
                return headers;
            }
        };
    }
}
