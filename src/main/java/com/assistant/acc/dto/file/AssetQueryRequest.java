package com.assistant.acc.dto.file;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AssetQueryRequest {
    private Integer projectNo;      // 프로젝트번호
    private String type;
}
