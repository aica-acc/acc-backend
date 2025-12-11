package com.assistant.acc.dto.report;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NoticeReportRequest {

    @JsonProperty("projectNo")
    private Integer projectNo;

    @JsonProperty("m_no")
    private String mNo;
}