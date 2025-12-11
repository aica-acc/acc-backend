package com.assistant.acc.domain.file;

import lombok.Data;

@Data
public class AssetDetail {
    private String fileUrl;
    private String fileName;
    private String extension;
    private Integer promptNo;
    private String visualPrompt;
    private String styleName;
}
