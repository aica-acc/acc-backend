package com.assistant.acc.service.editor;

import com.assistant.acc.domain.editor.EditorTemplate;

import java.util.List;
import java.util.Map;

public interface EditorTemplateService {

    void saveEditorTemplate(Integer pNo, String filePath);

    EditorTemplate getLatestTemplate(Integer pNo);
    
    List<Map<String, Object>> loadTemplateJson(String filePath);
}
