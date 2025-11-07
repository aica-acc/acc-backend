package com.assistant.acc.domain.project;

import lombok.Data;

@Data
public class UserInput {

    private Integer userInputNo;
    private Integer projectNo;

    // --- 1단계 (초기 입력) ---
    private String theme;
    private String keywords;
    private String title;

    // --- 1단계 (분석 결과 JSON) ---
    private String analysisSummary;
    private String posterTrendReport;
    private String strategyReport;

/*
    //getter setter 정리
    public Integer getUiNo() {
        return uiNo;
    }
    public void setUiNo(Integer uiNo) {
        this.uiNo = uiNo;
    }
    public Integer getPNo() {
        return pNo;
    }
    public void setPNo(Integer pNo) {
        this.pNo = pNo;
    }
    public String getTheme() {
        return theme;
    }
    public void setTheme(String theme) {
        this.theme = theme;
    }
    public String getKeywords() {
        return keywords;
    }
    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public String getAnalysisSummary() {
        return analysisSummary;
    }
    public void setAnalysisSummary(String analysisSummary) {
        this.analysisSummary = analysisSummary;
    }
    public String getPosterTrendReport() {
        return posterTrendReport;
    }
    public void setPosterTrendReport(String posterTrendReport) {
        this.posterTrendReport = posterTrendReport;
    }
    public String getStrategyReport() {
        return strategyReport;
    }
    public void setStrategyReport(String strategyReport) {
        this.strategyReport = strategyReport;
    }
*/

}
