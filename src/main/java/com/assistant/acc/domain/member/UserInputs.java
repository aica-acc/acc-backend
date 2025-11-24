package com.assistant.acc.domain.member;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInputs {
    private Integer userInputNo;
    private Integer pNo;
    private String theme;
    private String keywords;
    private String pName;
    private LocalDateTime createAt;
}