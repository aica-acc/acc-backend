package com.assistant.acc.service.editor;

import com.assistant.acc.domain.editor.EditorTemplate;
import com.assistant.acc.mapper.editor.EditorTemplateMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EditorTemplateServiceImpl implements EditorTemplateService {

    private final EditorTemplateMapper editorTemplateMapper;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Value("${python.api.url:http://localhost:5000}")
    private String pythonApiUrl;

    // AI 가 돌려주는 템플릿 경로에서 에디터 데이터 폴더 이후 부분 (…/data/editor/{runId}/layout/total.json)
    private static final String EDITOR_DIR_MARKER = "/data/editor/";

    @Override
    @Transactional
    public void saveEditorTemplate(Integer pNo, String filePath) {
        EditorTemplate template = new EditorTemplate();
        template.setPNo(pNo);
        template.setFilePath(filePath);

        log.info("📝 [EditorTemplateService] save pNo={}, filePath={}", pNo, filePath);
        editorTemplateMapper.insertEditorTemplate(template);
    }

    @Override
    public EditorTemplate getLatestTemplate(Integer pNo) {
        return editorTemplateMapper.selectLatestByProjectNo(pNo);
    }


    /**
     * 에디터 템플릿(total.json) 읽기.
     * 경로는 AI 서버 기준이다. 같은 디스크(로컬 개발)면 직접 읽고, 없으면(도커·서버 분리)
     * AI 서버의 정적 경로(/static/editor/...)로 받아온다. 백엔드는 AI 데이터 폴더를 마운트하지 않는다.
     */
    @Override
    public List<Map<String, Object>> loadTemplateJson(String filePath) {
        try {
            String jsonStr;
            Path local = Path.of(filePath);
            if (Files.isRegularFile(local)) {
                jsonStr = Files.readString(local);
            } else {
                String normalized = filePath.replace('\\', '/'); // Windows 경로 호환
                int idx = normalized.indexOf(EDITOR_DIR_MARKER);
                if (idx < 0) {
                    throw new IllegalStateException("에디터 데이터 경로가 아닙니다: " + filePath);
                }
                String url = pythonApiUrl.replaceAll("/+$", "") + "/static/editor/"
                        + normalized.substring(idx + EDITOR_DIR_MARKER.length());
                log.info("📥 [EditorTemplateService] 로컬에 없어 AI 서버에서 받음: {}", url);
                jsonStr = restTemplate.getForObject(url, String.class);
            }
            return objectMapper.readValue(jsonStr, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.error("❌ Failed to read template JSON file: {}", filePath, e);
            throw new RuntimeException("Failed to read template JSON", e);
        }
    }
}
