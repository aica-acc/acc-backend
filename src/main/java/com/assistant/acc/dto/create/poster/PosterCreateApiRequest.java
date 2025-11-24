package com.assistant.acc.dto.create.poster;

import com.assistant.acc.dto.create.PosterPromptOption;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PosterCreateApiRequest {
    @JsonProperty("analysis_summary")
    private Map<String, Object> analysisSummary;

    @JsonProperty("poster_trend_report")
    private Map<String, Object> posterTrendReport;

    @JsonProperty("strategy_report")
    private Map<String, Object> strategyReport;

    @JsonProperty("prompt_options")
    private List<PosterPromptOption> promptOptions;
}
