package com.assistant.acc.dto.editor;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class SaveImageRequest {
    @JsonProperty("pNo")
    private Integer pNo;
    
    @JsonProperty("imageBase64")
    private String imageBase64;  // "data:image/png;base64,..."
    
    @JsonProperty("dbFileType")
    private String dbFileType;   // "poster", "mascot", "banner" 등
}

