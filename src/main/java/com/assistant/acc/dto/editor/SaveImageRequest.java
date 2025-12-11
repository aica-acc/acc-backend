package com.assistant.acc.dto.editor;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class SaveImageRequest {
    @JsonProperty("pNo")
    private Integer pNo;
    
    @JsonProperty("imageBase64")
    private String imageBase64;  // "data:image/png;base64,..." (선택적, 하위 호환성 유지)
    
    @JsonProperty("imagePath")
    private String imagePath;    // 이미지 경로 (상대 경로: /data/promotion/... 또는 절대 경로)
    
    @JsonProperty("dbFileType")
    private String dbFileType;   // "poster", "mascot", "banner" 등
}

