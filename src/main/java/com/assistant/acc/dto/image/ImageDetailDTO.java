package com.assistant.acc.dto.image;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ImageDetailDTO {
    @JsonProperty("file_path_no")
    private Integer filePathNo;
    @JsonProperty("file_url")
    private String fileUrl;
    @JsonProperty("file_name")
    private String fileName;
    @JsonProperty("extension_name")
    private String extensionName;
    @JsonProperty("year")
    private Integer year;
    @JsonProperty("region")
    private String region;
    @JsonProperty("festival_name")
    private String festivalName;
    @JsonProperty("source_type")
    private String sourceType;
    @JsonProperty("create_at")
    private String createAt;
}
