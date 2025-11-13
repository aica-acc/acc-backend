package com.assistant.acc.utility;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ProposalFileDTO {
    private int proposalFilePathNo;     // PK
    private String userInputNo;         // FK
    private String proposalFilePath;    // 파일 경로
    private LocalDateTime createAt;     // 생성일
    private String proposalFileName;    // 파일 이름
    
}
