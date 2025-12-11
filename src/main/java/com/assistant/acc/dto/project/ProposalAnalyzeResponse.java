package com.assistant.acc.dto.project;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProposalAnalyzeResponse {

    private String status;

    @JsonProperty("analysis_summary")
    private ProposalAnalyze analysis;
}
