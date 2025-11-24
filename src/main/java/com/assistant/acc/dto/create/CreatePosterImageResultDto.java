package com.assistant.acc.dto.create;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreatePosterImageResultDto {
    private String status;

    @JsonProperty("images")
    private List<CreateImageResponseDto> images;

}
