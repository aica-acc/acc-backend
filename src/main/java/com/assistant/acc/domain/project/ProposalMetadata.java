package com.assistant.acc.domain.project;

import lombok.Data;
import java.util.Date;

@Data
public class ProposalMetadata {

    private Integer proposalMetadataNo;
    private Integer projectNo;

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
