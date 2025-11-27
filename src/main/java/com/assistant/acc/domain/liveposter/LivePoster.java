package com.assistant.acc.domain.liveposter;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class LivePoster {
    private Integer livePosterNo;   // PK
    private Integer posterNo;       // FK (원본 포스터 번호)
    private String taskId;       // 작업 티켓 번호
    private String filePath;     // 영상 파일 경로
    private String motionPrompt; // 모션 프롬프트 (생성 기록용)
    private LocalDateTime createAt;
    private Integer pNo;            // 프로젝트 번호
    private String aspectRatio;
}