package com.assistant.acc.dto.file;

import lombok.Data;

import java.util.List;

@Data
public class PythonRegenerateResponse {
    private List<PythonImageItem> images;
}
