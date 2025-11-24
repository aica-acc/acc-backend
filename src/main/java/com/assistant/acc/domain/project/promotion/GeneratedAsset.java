package com.assistant.acc.domain.project.promotion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeneratedAsset {
    private Integer generatedAssetNo;   // PK
    private Integer promotionNo;        // FK(promotion)
    private Integer promptNo;           // FK(prompt)
    private Integer isMain;             // 1=메인포스터, 0=파생물
    private String generateAssetType;   // 포스터 / 스크린도어 / 조명액자 등
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}