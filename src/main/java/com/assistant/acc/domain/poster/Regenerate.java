package com.assistant.acc.domain.poster;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Regenerate {
    private String memberNo;

    private Integer filePathNo;
    private Integer promptNo;
    private Integer generatedAssetNo;
    private Integer promotionNo;
    private Integer projectNo;

    private String oldFileName;
    private String oldFilePath;
    private String styleName;
    private String oldPrompt;

    private String newPrompt;

    private String newFileName;
    private String newFilePath;
    private String newImageUrl;
}
