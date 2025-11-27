package com.assistant.acc.service.editor;

import com.assistant.acc.dto.editor.EditorBuildResponse;
import com.assistant.acc.dto.editor.PythonBuildResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EditorBuildServiceImpl implements EditorBuildService {

    // ✅ FastAPI 엔드포인트
    private static final String PYTHON_EDITOR_BUILD_URL = "http://127.0.0.1:5000/editor/build";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final EditorTemplateService editorTemplateService;

    @Override
    public EditorBuildResponse buildAndSaveTemplates(int pNo, String postersJson) {
        try {
            // 1) postersJson 파싱 (List<Map<String, Object>>)
            List<Map<String, Object>> posters =
                    objectMapper.readValue(postersJson, new TypeReference<List<Map<String, Object>>>() {});

            // 2) Python에 보낼 payload 구성
            Map<String, Object> payload = new HashMap<>();
            payload.put("pNo", pNo);
            payload.put("posters", posters);

            log.info("🚀 [EditorBuildService] call Python editor.build, pNo={}, posters_count={}",
                    pNo, posters.size());

            // 3) FastAPI /editor/build 호출
            PythonBuildResponse pyResp = restTemplate.postForObject(
                    PYTHON_EDITOR_BUILD_URL,
                    payload,
                    PythonBuildResponse.class
            );

            if (pyResp == null) {
                throw new IllegalStateException("Python editor.build response is null");
            }

            // ✅ 새 응답 구조 기준 로그
            log.info("✅ [EditorBuildService] Python response status={}, pNo={}, filePath={}",
                    pyResp.getStatus(), pyResp.getPNo(), pyResp.getFilePath());

            // 4) 결과 filePath DB 저장
            String filePath = pyResp.getFilePath();
            if (filePath == null || filePath.isBlank()) {
                throw new IllegalStateException("Python editor.build returned empty filePath");
            }

            log.info("💾 [EditorBuildService] save template pNo={}, filePath={}", pNo, filePath);
            editorTemplateService.saveEditorTemplate(pNo, filePath);

            // 5) 프론트로 돌려줄 응답 (사실 안 써도 됨)
            EditorBuildResponse resp = new EditorBuildResponse();
            resp.setPNo(pNo);
            resp.setStatus(pyResp.getStatus() != null ? pyResp.getStatus() : "success");

            return resp;

        } catch (Exception e) {
            log.error("❌ [EditorBuildService] buildAndSaveTemplates error", e);
            throw new RuntimeException("Editor template build failed", e);
        }
    }
}
