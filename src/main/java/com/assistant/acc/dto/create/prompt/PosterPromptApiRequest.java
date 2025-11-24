package com.assistant.acc.dto.create.prompt;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PosterPromptApiRequest {

    private String theme;

    @JsonProperty("analysis_summary")
    private Map<String, Object> analysisSummary;

    @JsonProperty("poster_trend_report")
    private Map<String, Object> posterTrendReport;

    @JsonProperty("strategy_report")
    private Map<String, Object> strategyReport;
}