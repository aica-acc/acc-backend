package com.assistant.acc.dto.create.poster;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class PosterImageTextContentDto {

    private String title;

    private String subtitle;

    @JsonProperty("main_copy")
    private String mainCopy;

    @JsonProperty("date_location")
    private String dateLocation;

    private String programs;
}