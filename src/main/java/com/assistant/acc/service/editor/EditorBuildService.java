package com.assistant.acc.service.editor;

import com.assistant.acc.dto.editor.EditorBuildResponse;

public interface EditorBuildService {

    EditorBuildResponse buildAndSaveTemplates(int pNo, String postersJson);
}