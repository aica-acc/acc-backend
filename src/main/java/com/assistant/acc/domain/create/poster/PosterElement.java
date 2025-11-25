package com.assistant.acc.domain.create.poster;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PosterElement {
    private Integer filePathNo;       // generate_file_path_no (PK)
    private Integer generatedAssetNo; // generated_asset_no
    private Integer promotionNo;      // promotion_no
    private Integer promptNo;         // prompt_no
    private String visualPrompt;      // prompt.visual_prompt
    private String styleName;         // prompt.style_name

    private String fileUrl;           // generate_file_path.file_path
    private String fileName;          // generate_file_path.file_name
    private String extension;         // generate_file_path.extension

    private String generateAssetType; // '포스터', '마스코트', etc.
    private Integer projectNo;              // project_no
    @Builder.Default
    private String memberNo="M000001" ;               // member_no

}
