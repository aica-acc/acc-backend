package com.assistant.acc.mapper.project;

import com.assistant.acc.domain.project.ProposalMetadata;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ProposalMetadataMapper {
    // 프로젝트 번호로 기획서 메타데이터(제목, 일시 등) 조회
    ProposalMetadata findByPNo(@Param("pNo") Integer pNo);
}
