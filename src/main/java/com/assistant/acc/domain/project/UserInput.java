package com.assistant.acc.domain.project;

import lombok.Data;

@Data
public class UserInput {

    private Integer userInputNo;
    private Integer projectNo;

    // --- 1단계 (초기 입력) ---
    private String theme;
    private String keywords;
    private String pName;

    // --- 1단계 (분석 결과 JSON) ---
    private String analysisSummary;
    private String posterTrendReport;
    private String strategyReport;

}
