package com.assistant.acc.service.editor;

import com.assistant.acc.dto.editor.EditorBuildResponse;

public interface EditorBuildService {

    default EditorBuildResponse buildAndSaveTemplates(int pNo, String postersJson) {
        return buildAndSaveTemplates(pNo, postersJson, false);
    }

    /** demo=true: 데모(test) 계정. AI 가 이미 만든 결과만 쓰고 새로 생성하지 않는다 */
    EditorBuildResponse buildAndSaveTemplates(int pNo, String postersJson, boolean demo);
}
