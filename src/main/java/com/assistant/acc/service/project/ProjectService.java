package com.assistant.acc.service.project;

import org.springframework.web.multipart.MultipartFile;
import com.assistant.acc.domain.project.Project;
import com.assistant.acc.domain.project.ProposalMetadata;

/**
 * 축제 정보를 가져오기 위해 메서드를 정의하기 위한 인터페이스
 */
public interface ProjectService {

    /**
     * 새 프로젝트를 생성하고, 사용자의 초기 입력을 저장합니다.
     */
    public Project createProjectAndSaveInput(String theme, String keywords, String title, String memberId);

    /**
     * AI 분석이 완료된 Proposal 메타데이터를 DB에 저장합니다.
     */
    public void saveProposalMetadata(ProposalMetadata metadata);

    /**
     * ProposalMEtadata조회
     */
    public ProposalMetadata getLatestProposalMetadata();



}
