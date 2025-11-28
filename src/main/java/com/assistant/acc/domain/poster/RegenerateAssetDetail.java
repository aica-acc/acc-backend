package com.assistant.acc.domain.poster;

import lombok.Data;

/**
 * DB 초기 조회 전용 도메인 객체
 */
@Data
public class RegenerateAssetDetail {
    private Integer filePathNo;
    private String fileUrl;
    private String fileName;
    private String extension;

    private Integer promptNo;
    private String visualPrompt;
    private String styleName;

    private Integer generatedAssetNo;
    private Integer promotionNo;
    private Integer projectNo;
    private String memberNo;
}
