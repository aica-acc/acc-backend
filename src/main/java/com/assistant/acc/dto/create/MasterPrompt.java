package com.assistant.acc.dto.create;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class MasterPrompt {
    @JsonProperty("prompt_options")
    private List<PosterPromptOption> promptOptions;
}
