package com.assistant.acc.dto.liveposter;

import lombok.Data;

@Data
public class LivePosterRequestDTO {
    // [Java -> Python] 요청 객체
    private Integer projectId;
    private String posterImagePath;
    private String conceptText;
    private String visualKeywords;
}
