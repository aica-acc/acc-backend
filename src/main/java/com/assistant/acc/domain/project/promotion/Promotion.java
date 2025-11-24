package com.assistant.acc.domain.project.promotion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Promotion {
    private Integer promotionNo;
    private Integer pNo;
    private Integer promptNo;
    private String promotionType; // 포스터 / 마스코트 / 기타
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}