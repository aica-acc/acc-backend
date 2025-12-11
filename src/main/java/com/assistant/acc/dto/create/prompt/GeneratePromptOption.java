package com.assistant.acc.dto.create.prompt;

import com.assistant.acc.dto.create.poster.PosterImageTextContentDto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeneratePromptOption {

    @JsonProperty("style_name")
    private String styleName;

    @JsonProperty("visual_prompt")
    private String visualPrompt;

    @JsonProperty("visual_prompt_for_background")
    private String visualPromptForBackground;

    @JsonProperty("suggested_text_style")
    private String suggestedTextStyle;

    @JsonProperty("text_content")
    private PosterImageTextContentDto textContent;

}
