package com.assistant.acc.dto.editor;

import java.util.List;

import lombok.Data;

@Data
public class PythonBuildResponse {

    private String status;
    private Integer pNo;
    private String filePath;
    private List<String> dbFilePath;
    private List<String> dbFileType;

}
