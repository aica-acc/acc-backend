package com.assistant.acc.dto.create;

import com.assistant.acc.domain.project.ProposalMetadata;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateImageRequestDto {
    @JsonProperty("prompt_options")
    private List<SelectedPromptDataDto> promptOptions;

    @JsonProperty("analysis_summary")
    private ProposalMetadata analysisSummary;
}