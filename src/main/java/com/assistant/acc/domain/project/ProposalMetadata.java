package com.assistant.acc.domain.project;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.Date;

@Data
public class ProposalMetadata {

    private Integer proposalMetadataNo;
    private Integer projectNo;
    
    // JSON 직렬화 시 pNo 필드도 포함 (프론트엔드 호환성)
    @JsonProperty("pNo")
    public Integer getPNo() {
        return projectNo;
    }

    private String title;
    private Date festivalStartDate;
    private Date festivalEndDate;
    private String location;
    private String host;
    private String organizer;
    private String target;
    private String contactInfo;
    private String directions;

    //json
    private String programName;
    private String eventName;
    private String visualKeywords;
    private String conceptDescription;
    private Date createAt;

}
