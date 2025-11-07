package com.assistant.acc.dto.banner;

public class BannerPromptRequest {
    // 파이썬 실행 파일 경로 (예: "python" 또는 "C:\\Python312\\python.exe")
    private String pythonExe;

    // 파이썬 스크립트 경로 (반드시 절대경로 권장)
    private String scriptPath;

    // analysis.json 경로 (절대경로 권장)
    private String analysisPath;

    // 아래 값들은 비워두면 파이썬 스크립트의 기본값(엔터 입력)으로 처리
    private Integer width;
    private Integer height;
    private String aspectRatio;
    private String resolution;
    private Boolean usePreLlm;
    private Integer seed;

    public String getPythonExe() { return pythonExe; }
    public void setPythonExe(String pythonExe) { this.pythonExe = pythonExe; }

    public String getScriptPath() { return scriptPath; }
    public void setScriptPath(String scriptPath) { this.scriptPath = scriptPath; }

    public String getAnalysisPath() { return analysisPath; }
    public void setAnalysisPath(String analysisPath) { this.analysisPath = analysisPath; }

    public Integer getWidth() { return width; }
    public void setWidth(Integer width) { this.width = width; }

    public Integer getHeight() { return height; }
    public void setHeight(Integer height) { this.height = height; }

    public String getAspectRatio() { return aspectRatio; }
    public void setAspectRatio(String aspectRatio) { this.aspectRatio = aspectRatio; }

    public String getResolution() { return resolution; }
    public void setResolution(String resolution) { this.resolution = resolution; }

    public Boolean getUsePreLlm() { return usePreLlm; }
    public void setUsePreLlm(Boolean usePreLlm) { this.usePreLlm = usePreLlm; }

    public Integer getSeed() { return seed; }
    public void setSeed(Integer seed) { this.seed = seed; }
}
