package com.assistant.acc.config.web;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import com.assistant.acc.security.DemoBlockedException;

/**
 * ResponseStatusException 의 사유(사용자용 문장)를 응답 본문 message 로 내보낸다.
 * 기본 오류 응답은 메시지를 빼서 화면이 "크레딧 부족" 같은 안내를 할 수 없었다.
 * code: DEMO(데모 계정 제한), CREDIT(크레딧 부족)
 */
@RestControllerAdvice
public class ApiErrorAdvice {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handle(ResponseStatusException e) {
        Map<String, Object> body = new LinkedHashMap<>();
        int status = e.getStatusCode().value();
        body.put("status", status);
        if (e instanceof DemoBlockedException) {
            body.put("code", "DEMO");
        } else if (status == HttpStatus.PAYMENT_REQUIRED.value()) {
            body.put("code", "CREDIT");
        }
        body.put("message", e.getReason());
        return ResponseEntity.status(e.getStatusCode()).body(body);
    }
}
