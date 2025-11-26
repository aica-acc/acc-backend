package com.assistant.acc.domain.poster;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RegenerateResponseDTO {
    private Integer filePathNo;
    private Integer promptNo;

    private String fileUrl;
    private String fileName;
    private String extension;

    private String visualPrompt;
    private String styleName;

    private String newImageUrl;

    private boolean success;
}