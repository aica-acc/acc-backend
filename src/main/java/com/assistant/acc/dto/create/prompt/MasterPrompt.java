package com.assistant.acc.dto.create.prompt;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class MasterPrompt {
    @JsonProperty("prompt_options")
    private List<PosterPromptOption> promptOptions;
}
