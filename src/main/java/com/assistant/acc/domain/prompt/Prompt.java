package com.assistant.acc.domain.prompt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Prompt {
    private Integer promptNo;
    private  Integer userInputNo;
    private String visualPrompt;
    private String styleName;
    private LocalDateTime createdAt;
}
