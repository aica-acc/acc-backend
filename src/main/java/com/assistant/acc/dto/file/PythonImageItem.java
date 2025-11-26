package com.assistant.acc.dto.file;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PythonImageItem {
    private String image_url;
    private String style_name;
    private String visual_prompt;
    private Object text_content;
    private String file_path;
}
