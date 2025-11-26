package com.assistant.acc.service.editor;

import com.assistant.acc.domain.editor.EditorTemplate;
import com.assistant.acc.mapper.editor.EditorTemplateMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
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


    @Override
    public List<Map<String, Object>> loadTemplateJson(String filePath) {
        try {
            String jsonStr = Files.readString(Path.of(filePath));
            return objectMapper.readValue(jsonStr, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.error("❌ Failed to read template JSON file: {}", filePath, e);
            throw new RuntimeException("Failed to read template JSON", e);
        }
    }
}
