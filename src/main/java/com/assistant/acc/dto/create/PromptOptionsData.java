package com.assistant.acc.dto.create;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PromptOptionsData {
    private String status;

    @JsonProperty("master_prompt")
    private MasterPrompt masterPrompt;
}
