package com.assistant.acc.dto.file;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AssetDetailRequestDto {
    private Integer filePathNo;
    private Integer promptNo;
}
