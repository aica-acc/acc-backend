package com.assistant.acc.dto.image;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// 재생성 결과용
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ImageRegenerateResponseDTO {
    @JsonProperty("file_path_no")
    private Integer filePathNo;
    @JsonProperty("file_url")
    private String fileUrl;
    @JsonProperty("visual_prompt")
    private String visualPrompt;
    @JsonProperty("regenerated")
    private boolean regenerated;
    @JsonProperty("message")
    private String message;
}
