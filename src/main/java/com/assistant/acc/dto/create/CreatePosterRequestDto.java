package com.assistant.acc.dto.create;

import com.assistant.acc.domain.project.ProposalMetadata;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreatePosterRequestDto {
    @JsonProperty("analysis_summary")
    private ProposalMetadata analysisSummary;

    @JsonProperty("poster_trend_report")
    private Map<String, Object> posterTrendReport;  // 얘는 세션값을 받아와야함.

    @JsonProperty("strategy_report")
    private Map<String, Object> strategyReport;

    @JsonProperty("prompt_options")
    private List<PosterPromptOption> promptOptions;

}
