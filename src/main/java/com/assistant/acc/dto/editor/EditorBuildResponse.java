package com.assistant.acc.dto.editor;

import lombok.Data;

@Data
public class EditorBuildResponse {

    // 어떤 프로젝트에 대한 빌드였는지 정도만
    private Integer pNo;

    // "success" / "failed" 등
    private String status;
}
