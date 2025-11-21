package com.assistant.acc.dto.poster;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;


@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PosterCreateResponse {

    private String status; // success

    // 파이썬이 "images"라는 키로 리스트를 준다.
    @JsonProperty("images")
    private List<GeneratedImage> images;

    // --- [내부 클래스] 생성된 이미지 정보 ---
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GeneratedImage {

        @JsonProperty("style_name")
        private String styleName;

        @JsonProperty("image_url")
        private String imageUrl; // "/poster-images/..."

        @JsonProperty("visual_prompt")
        private String visualPrompt;

        @JsonProperty("text_content")
        private Map<String, String> textContent;
    }
}
