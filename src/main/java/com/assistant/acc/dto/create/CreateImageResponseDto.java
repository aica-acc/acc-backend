package com.assistant.acc.dto.create;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateImageResponseDto {
    @JsonProperty("style_name")
    private String styleName;

    @JsonProperty("image_url")
    private String imageUrl;

    @JsonProperty("visual_prompt")
    private String visualPrompt;

    @JsonProperty("text_content")
    private Map<String, Object> textContent;

    @JsonProperty("file_path")
    private String filePath;
}
