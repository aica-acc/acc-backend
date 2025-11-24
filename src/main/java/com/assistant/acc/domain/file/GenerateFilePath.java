package com.assistant.acc.domain.file;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenerateFilePath {
    private Integer generateFilePathNo; // PK
    private Integer generatedAssetNo;   // FK
    private String filePath;            // /generated/m_no/p_no/poster/...
    private String fileName;            // xxx_12345
    private String extension;           // png, jpg
    private LocalDateTime createdAt;
}