package com.assistant.acc.dto.poster;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PosterPromptResponse {

    private String status; // success

    // JSON의 "prompt_options_data" 키와
    @JsonProperty("prompt_options_data")
    private PromptOptionData promptOptionsData;

    // --- [내부 클래스 1] 중간 데이터 ---
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PromptOptionData {
        private String status;

        // JSON의 "master_prompt" 키와 매핑
        @JsonProperty("master_prompt")
        private MasterPrompt masterPrompt;
    }

    // --- [내부 클래스 2] 마스터 프롬프트 ---
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MasterPrompt {

        // JSON의 prompt_options (리스트) 키와 매핑
        @JsonProperty("prompt_options")
        private List<PosterOption> promptOptions;
    }

    // --- [내부 클래스 3] 개별 포스터 옵션 ---
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PosterOption {

        @JsonProperty("style_name")
        private String styleName;      // 예: "Cosmic Wonder"

        @JsonProperty("visual_prompt")
        private String visualPrompt;   // 영어 프롬프트

        @JsonProperty("visual_prompt_for_background")
        private String visualPromptForBackground; // (없어도 에러 안 남)

        @JsonProperty("text_content")
        private Map<String, String> textContent; // { "title": "...", "date": "..." }
    }
}