package com.assistant.acc.dto.project;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProposalAnalyze {
  
    private String title;
    private String location;
    private String host;
    private String organizer;

    @JsonProperty("targetAudience")
    private String targetAudience;

    @JsonProperty("contactInfo")
    private String contactInfo;

    private String directions;
    private String date;

    private List<String> programs;
    private List<String> events;
    private String summary;

    @JsonProperty("visualKeywords")
    private List<String> visualKeywords;
}