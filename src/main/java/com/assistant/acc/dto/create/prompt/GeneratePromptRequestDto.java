package com.assistant.acc.dto.create.prompt;

import com.assistant.acc.domain.project.ProposalMetadata;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeneratePromptRequestDto {
    // p_no를 통해서 user_input 테이블 조회하면 theme 받아 올 수 있음.
    private String theme;

    @JsonProperty("analysis_summary")
    private ProposalMetadata analysisSummary;

    @JsonProperty("poster_trend_report")
    private Map<String, Object> posterTrendReport;

    @JsonProperty("strategy_report")
    private Map<String, Object> strategyReport;
}
