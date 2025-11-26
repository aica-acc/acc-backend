package com.assistant.acc.dto.editor;

import lombok.Data;

@Data
public class PythonBuildResponse {

    private String status;
    private Integer pNo;
    private String filePath;

}
