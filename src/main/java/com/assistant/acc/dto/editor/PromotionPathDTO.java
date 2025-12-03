package com.assistant.acc.dto.editor;


import java.time.LocalDate;
import lombok.Data;


@Data
public class PromotionPathDTO {
    private Integer promotionPathNo;
    private Integer pNo;
    private String dbFilePath;
    private String dbFileType;
    private LocalDate createAt;
}