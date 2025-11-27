package com.assistant.acc.dto.liveposter;

import lombok.Data;

@Data
public class LivePosterResponseDTO {
    //(Python → Java 응답용)
    private String taskId;
    private String filePath;
    private String motionPrompt;
    private String aspectRatio;
}
