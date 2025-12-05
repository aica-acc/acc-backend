package com.assistant.acc.dto.report;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ArticleReportRequest {

    // 프론트엔드에서 보내는 "projectNo"
    @JsonProperty("projectNo")
    private Integer projectNo;

    // BaseAPI가 자동으로 보내는 "m_no" (이거 없으면 400 에러 남)
    @JsonProperty("m_no")
    private String mNo;
}