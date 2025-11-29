package com.assistant.acc.dto.liveposter;

import com.fasterxml.jackson.annotation.JsonProperty; // 👈 이 import가 핵심입니다!
import lombok.Data;

@Data
public class LivePosterResponseDTO {

    @JsonProperty("task_id")
    private String taskId;

    @JsonProperty("file_path")
    private String filePath;

    @JsonProperty("motion_prompt")
    private String motionPrompt;

    @JsonProperty("aspect_ratio")
    private String aspectRatio;
}