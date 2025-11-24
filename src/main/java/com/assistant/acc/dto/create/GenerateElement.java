package com.assistant.acc.dto.create;

import com.assistant.acc.domain.member.UserInputs;
import com.assistant.acc.domain.project.ProposalMetadata;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GenerateElement {
    private Integer projectNo;
    private UserInputs userInputs;
    private ProposalMetadata proposalMetadata;
}
