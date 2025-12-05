package com.assistant.acc.dto.liveposter;

import com.fasterxml.jackson.annotation.JsonProperty; // 이 import 필수!
import lombok.Data;

@Data
public class LivePosterRequestDTO {

    @JsonProperty("project_id")
    private Integer projectId;

    @JsonProperty("poster_image_path")
    private String posterImagePath;

    @JsonProperty("concept_text")
    private String conceptText;

    @JsonProperty("visual_keywords")
    private String visualKeywords;
}