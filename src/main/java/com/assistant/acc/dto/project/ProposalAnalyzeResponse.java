package com.assistant.acc.dto.project;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProposalAnalyzeResponse {

    private String status;
    private ProposalAnalyze analysis;
}
