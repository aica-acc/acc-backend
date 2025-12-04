package com.assistant.acc.dto.editor;

import lombok.Data;

@Data
public class SaveImageResponse {
    private Boolean success;
    private String savedPath;  // "/data/promotion/m000001/{pNo}/editor/..."
    private String message;
}

