
package com.assistant.acc.service.editor;

import org.springframework.beans.factory.annotation.Value;
import com.assistant.acc.dto.editor.EditorAiRenderRequest;
import com.assistant.acc.dto.editor.EditorAiRenderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Editor → Python AI 서버 호출 담당 서비스.
 * AI 색상 추천(스타일링) 기능: 캔버스 텍스트 객체의 스타일을 추천받아 변경된 canvasData 반환.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EditorAiRenderServiceImpl implements EditorAiRenderService {

    // AI 서버 URL: /editor/render 엔드포인트
    @Value("${python.api.url:http://localhost:5000}")
    private String pythonApiUrl;

    @Override
    public EditorAiRenderResponse renderWithAi(EditorAiRenderRequest request) {

        try {
            // RestTemplate 타임아웃 설정 (AI 처리 시간이 오래 걸릴 수 있음)
            RestTemplate restTemplate = new RestTemplate();
            
            // 타임아웃 설정 (60초)
            org.springframework.http.client.SimpleClientHttpRequestFactory factory = 
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(10000); // 10초
            factory.setReadTimeout(60000); // 60초
            restTemplate.setRequestFactory(factory);

            // 필수 필드 검증
            if (request.getBackgroundImage() == null || request.getBackgroundImage().isEmpty()) {
                EditorAiRenderResponse error = new EditorAiRenderResponse();
                error.setStatus("error");
                error.setMessage("backgroundImage가 없습니다.");
                return error;
            }
            
            if (request.getCanvasJson() == null || request.getCanvasJson().isEmpty()) {
                EditorAiRenderResponse error = new EditorAiRenderResponse();
                error.setStatus("error");
                error.setMessage("canvasJson이 없습니다.");
                return error;
            }

            // AI 서버에 넘길 payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("backgroundImage", request.getBackgroundImage()); 
            payload.put("canvasJson", request.getCanvasJson()); 
            payload.put("layoutType", request.getLayoutType() != null ? request.getLayoutType() : "default");

            log.info("🎨 [EditorAiRenderService] AI 색상 추천 요청 URL={}, layoutType={}",
                    pythonApiUrl + "/editor/render", request.getLayoutType());

            // AI 서버 응답: { status: "success", updatedCanvas: {...} }
            ResponseEntity<Map> responseEntity =
                    restTemplate.postForEntity(
                            pythonApiUrl + "/editor/render",
                            payload,
                            Map.class
                    );

            Map<String, Object> responseBody = responseEntity.getBody();

            if (responseBody == null) {
                EditorAiRenderResponse error = new EditorAiRenderResponse();
                error.setStatus("error");
                error.setMessage("empty response from python ai server");
                return error;
            }

            // AI 서버 응답을 EditorAiRenderResponse로 변환
            EditorAiRenderResponse response = new EditorAiRenderResponse();
            response.setStatus((String) responseBody.get("status"));
            
            // updatedCanvas가 null인 경우 처리
            Object updatedCanvasObj = responseBody.get("updatedCanvas");
            if (updatedCanvasObj != null && updatedCanvasObj instanceof Map) {
                response.setUpdatedCanvas((Map<String, Object>) updatedCanvasObj);
            } else {
                log.warn("⚠️ [EditorAiRenderService] updatedCanvas가 null이거나 Map이 아닙니다: {}", updatedCanvasObj);
                response.setUpdatedCanvas(null);
            }
            
            if (response.getStatus() == null || !response.getStatus().equals("success")) {
                response.setMessage((String) responseBody.getOrDefault("message", "AI 서버 오류"));
            }

            log.info("✅ [EditorAiRenderService] AI 색상 추천 완료, status={}", response.getStatus());
            return response;

        } catch (org.springframework.web.client.ResourceAccessException e) {
            log.error("❌ [EditorAiRenderService] AI 서버 연결 실패 (타임아웃 또는 네트워크 오류)", e);
            EditorAiRenderResponse error = new EditorAiRenderResponse();
            error.setStatus("error");
            error.setMessage("AI 서버 연결 실패: " + e.getMessage());
            return error;
        } catch (Exception e) {
            log.error("❌ [EditorAiRenderService] renderWithAi error", e);
            EditorAiRenderResponse error = new EditorAiRenderResponse();
            error.setStatus("error");
            error.setMessage("AI render failed: " + e.getMessage());
            return error;
        }
    }
}
