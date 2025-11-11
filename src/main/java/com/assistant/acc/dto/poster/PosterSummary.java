package com.assistant.acc.dto.poster;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PosterSummary {


    private String title;
    private String location;
    private String host;
    private String organizer;
    private String targetAudience;
    private String contactInfo;
    private String directions;
    private String date;

    private List<String> programs;
    private List<String> events;
    private List<String> visualKeywords;

}
