package com.assistant.acc.dto.create.poster;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class CreateImageResultResponse {
    private String status;

    @JsonProperty("images")
    private List<CreateImageResponseDto> images;
}