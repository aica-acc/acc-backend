package com.assistant.acc.dto.create;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SelectedPromptDataDto {

    @JsonProperty("style_name")
    private String styleName;

    @JsonProperty("visual_prompt")
    private String visualPrompt;

    @JsonProperty("visual_prompt_for_background")
    private String visualPromptForBackground;

    @JsonProperty("suggested_text_style")
    private String suggestedTextStyle = "default";

    private int width;
    private int height;

    @JsonProperty("text_content")
    private PosterImageTextContentDto textContent;
}