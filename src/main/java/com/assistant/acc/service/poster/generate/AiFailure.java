package com.assistant.acc.service.poster.generate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * AI 이미지 생성 실패를 사용자용 사유로 바꾼다 (#31).
 *
 * AI 는 일부라도 실패하면 502 와 {"detail": {"message", "failed": [{index, style_name, error}], "succeeded"}} 를 보낸다.
 * 사유(몇 번째 장이 실패했는지)는 generation_job.error 에 남고, 원문(모델 오류)은 로그에만 남긴다.
 * 성공한 장은 AI 의 요청 폴더에 남아 있어 다시 시도하면 실패한 장만 다시 만든다.
 */
@Slf4j
final class AiFailure {

    private static final ObjectMapper JSON = new ObjectMapper();

    private AiFailure() {
    }

    static ResponseStatusException of(RuntimeException e, String label) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof HttpStatusCodeException http) {
                return fromResponse(http, label);
            }
            if (t instanceof ResourceAccessException) {
                // 연결이 끊겨도 AI 가 끝까지 만들었을 수 있다. 같은 요청 폴더라 다시 시도하면 그 결과를 쓴다
                log.warn("[ai] {} 생성 중 연결 끊김: {}", label, t.getMessage());
                return new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "AI 서버와 연결이 끊겼습니다. 다시 시도하면 이미 만든 " + label + "는 그대로 씁니다.");
            }
        }
        log.error("[ai] {} 생성 실패", label, e);
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, label + " 생성 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");
    }

    @SuppressWarnings("unchecked")
    private static ResponseStatusException fromResponse(HttpStatusCodeException http, String label) {
        try {
            Map<String, Object> body = JSON.readValue(http.getResponseBodyAsString(), Map.class);
            Object detail = body.get("detail");
            if (detail instanceof Map<?, ?> d && d.get("failed") instanceof List<?> failed && !failed.isEmpty()) {
                List<Map<String, Object>> items = (List<Map<String, Object>>) failed;
                int total = items.size() + (d.get("succeeded") instanceof List<?> ok ? ok.size() : 0);
                items.forEach(f -> log.warn("[ai] {} {}번째({}) 실패: {}", label,
                        ((Number) f.get("index")).intValue() + 1, f.get("style_name"), f.get("error")));
                String which = items.stream()
                        .map(f -> (((Number) f.get("index")).intValue() + 1) + "번")
                        .collect(Collectors.joining(", "));
                return new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        label + " " + total + "장 중 " + items.size() + "장(" + which + ")을 만들지 못했습니다. "
                                + "다시 시도하면 실패한 장만 다시 만듭니다.");
            }
        } catch (Exception ignore) {
            // 형식이 다른 오류 응답은 아래 기본 문구
        }
        log.error("[ai] {} 생성 실패 {}: {}", label, http.getStatusCode(), http.getResponseBodyAsString());
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, label + " 생성에 실패했습니다. 잠시 후 다시 시도해 주세요.");
    }
}
