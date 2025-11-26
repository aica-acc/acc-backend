
package com.assistant.acc.service.editor;

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
 * Editor → Python AI 서버(Gemini 3 Pro Image) 호출 담당 서비스.
 * DB 저장은 안 하고, 파이썬에서 만들어준 imageUrl 그대로 리턴만 한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EditorAiRenderServiceImpl implements EditorAiRenderService {

    // TODO: 필요하면 application.yml로 빼기
    private static final String PYTHON_AI_RENDER_URL = "http://127.0.0.1:5000/ai/editor/render";

    @Override
    public EditorAiRenderResponse renderWithAi(EditorAiRenderRequest request) {

        try {
            RestTemplate restTemplate = new RestTemplate();

            // 파이썬 쪽에 넘길 payload (필요하면 키 이름 맞게 조정)
            Map<String, Object> payload = new HashMap<>();
            payload.put("pNo", request.getPNo());
            payload.put("layoutType", request.getLayoutType());
            payload.put("backgroundImage", request.getBackgroundImage()); 
            payload.put("canvasJson", request.getCanvasJson()); 

            log.info("🚀 [EditorAiRenderService] call Python AI URL={}, pNo={}, layoutType={}, model={}",
                    PYTHON_AI_RENDER_URL, request.getPNo(), request.getLayoutType(), request.getModel());

            ResponseEntity<EditorAiRenderResponse> responseEntity =
                    restTemplate.postForEntity(
                            PYTHON_AI_RENDER_URL,
                            payload,
                            EditorAiRenderResponse.class
                    );

            EditorAiRenderResponse body = responseEntity.getBody();

            if (body == null) {
                EditorAiRenderResponse error = new EditorAiRenderResponse();
                error.setStatus("error");
                error.setMessage("empty response from python ai server");
                return error;
            }

            return body;

        } catch (Exception e) {
            log.error("❌ [EditorAiRenderService] renderWithAi error", e);
            EditorAiRenderResponse error = new EditorAiRenderResponse();
            error.setStatus("error");
            error.setMessage("AI render failed: " + e.getMessage());
            return error;
        }
    }
}
