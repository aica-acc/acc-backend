package com.assistant.acc.dto.create.poster;

import com.assistant.acc.domain.project.ProposalMetadata;
import com.assistant.acc.dto.create.prompt.SelectedPromptDataDto;
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

    /** AI 결과 폴더 키 (AiPromotionFiles.batchOf). 같은 키로 다시 요청하면 AI 는 이미 만든 장을 다시 만들지 않는다 */
    private String batch;
}