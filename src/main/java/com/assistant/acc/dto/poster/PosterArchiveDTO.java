package com.assistant.acc.dto.poster;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class PosterArchiveDTO {
    private Integer filePathNo;   // PK
    private Integer projectNo;    // FK
    private String fileName;
    private String fileUrl;       // 이미지 주소
    private String visualPrompt;
    private String styleName;
    private Date createAt;

    //CSV 파일내 컬럼 기반으로 생성
    private String id;
    private Integer year;
    private String region;
    private String festivalName;
    // 변환된 웹 주소
    private String imageUrl;
    private Double aesthetic;
    private String aestheticDescription;
    private Double thematic;
    private String thematicDescription;
    private Double readability;
    private String readabilityDescription;
    private Double creativity;
    private String creativityDescription;
}
