package com.assistant.acc.dto.poster;

import lombok.Data;

// front -> back
@Data
public class PosterPromptRequest {
    private String title;
    private String theme;
    private String keywords;
}
