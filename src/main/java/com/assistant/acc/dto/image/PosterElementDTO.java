package com.assistant.acc.dto.image;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PosterElementDTO {
    @JsonProperty("file_path_no")
    private Integer filePathNo;
    @JsonProperty("poster_prompt_no")
    private Integer posterPromptNo;
    @JsonProperty("visual_prompt")
    private String visualPrompt;

    private String fileUrl;
    private String assetType;
    private Integer isMain;
}
