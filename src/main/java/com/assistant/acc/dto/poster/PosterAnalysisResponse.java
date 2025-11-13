package com.assistant.acc.dto.poster;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty; // ⭐️ 1. 이 import 문을 추가합니다.
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PosterAnalysisResponse {

    private String status;
    private String message;

    // ⭐️ 2. "JSON 키"와 "Java 필드"를 수동으로 연결합니다.
    @JsonProperty("analysis_summary")
    private PosterSummary analysis_summary;

    // ⭐️ 3. strategy_report도 동일하게 수정합니다.
    @JsonProperty("strategy_report")
    private PosterStrategy strategy_report;

    // ⭐️ 4. expanded_keywords도 동일하게 수정합니다.
    @JsonProperty("expanded_keywords")
    private String[] expanded_keywords;
}