package com.assistant.acc.dto.create.prompt;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Collections;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreatePromptResponseDto {
    private String status;  // "success"

    @JsonProperty("prompt_options_data")
    private PromptOptionsData promptOptionsData;

    public String getStatus() {
        return status;
    }

    public PromptOptionsData getPromptOptionsData() {
        return promptOptionsData;
    }

    // 👉 사용 편하게: 바로 List만 뽑는 헬퍼 메서드
    @JsonIgnore
    public List<PosterPromptOption> getPromptOptions() {
        if (promptOptionsData == null ||
                promptOptionsData.getMasterPrompt() == null ||
                promptOptionsData.getMasterPrompt().getPromptOptions() == null) {
            return Collections.emptyList();
        }
        return promptOptionsData.getMasterPrompt().getPromptOptions();
    }
}