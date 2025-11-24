package com.assistant.acc.dto.create.prompt;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PosterTextContent {
    @JsonProperty("title")
    private String title;

    @JsonProperty("date_location")
    private String dateLocation;
}
