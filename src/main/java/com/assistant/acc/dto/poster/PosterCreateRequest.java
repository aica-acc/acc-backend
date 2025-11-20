package com.assistant.acc.dto.poster;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class PosterCreateRequest {

    // 1단계 결과였던 "analysis_summary"를 통째로 백업해서 보냄
    @JsonProperty("analysis_summary")
    private Map<String, Object> analysisSummary;

    // 2단계 결과인 옵션 리스트를 보냄
    @JsonProperty("prompt_options")
    private List<PosterPromptResponse.PosterOption> promptOptions;
}
