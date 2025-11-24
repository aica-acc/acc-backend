package com.assistant.acc.dto.create.poster;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PosterCreateImageResponse {

    private String status;

    @JsonProperty("images")
    private List<GeneratedImage> images;

    @Data
    public static class GeneratedImage {

        @JsonProperty("style_name")
        private String styleName;

        @JsonProperty("image_url")
        private String imageUrl;

        @JsonProperty("visual_prompt")
        private String visualPrompt;

        @JsonProperty("text_content")
        private Map<String, Object> textContent;
    }
}