package com.assistant.acc.utility;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProposalMapper {
    int insertProposalFile(ProposalFileDTO dto);
    
}
