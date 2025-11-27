package com.assistant.acc.domain.file;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AssetElement {
    private Integer filePathNo;        // 상세 조회 키
    private Integer promptNo;          // 상세 조회 키
    private Integer generatedAssetNo;  // 정렬 기준
}
