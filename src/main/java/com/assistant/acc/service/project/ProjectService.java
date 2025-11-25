package com.assistant.acc.service.project;

import java.io.IOException;
import java.util.Map;

import com.assistant.acc.dto.project.RegionTrendResponseDTO;
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
     * 포스터 파일과 사용자 입력을 받아 AI 분석을 요청하고 결과를 반환합니다.
     * (프로젝트 생성 및 저장은 ProjectService에게 위임합니다)
     *
     * @param file     사용자가 업로드한 포스터 (PDF 또는 이미지)
     * @param theme    축제 주제
     * @param keywords 축제 키워드
     * @param title    축제 제목
     * @return AI 서버가 반환한 원본 JSON 문자열
     * @throws IOException AI 서버 통신 또는 파일 처리 중 예외 발생 시
     */
    ProposalMetadata analyzeProposal(MultipartFile file, String theme, String keywords, String title) throws IOException;
    /**
     * 2단계: AI 프롬프트 생성을 Python 서버에 요청
     * (JSON을 받아 JSON으로 반환한다)
     *
     * @param jsonBody 1단계 분석 결과가 담긴 원본 JSON 문자열
     * @return AI 서버가 반환한 프롬프트 시안(JSON 문자열)
     * @throws IOException
     */


    /**
     * AI 분석이 완료된 Proposal 메타데이터를 DB에 저장합니다.
     */
    public void saveProposalMetadata(ProposalMetadata metadata);

    /**
     * ProposalMEtadata조회
     */
    public ProposalMetadata getLatestProposalMetadata();

    public  Map<String, Object> analyzeTotalTrend(String keyword, String title, String festivalStartDate) throws IOException;

    RegionTrendResponseDTO analyzeRegionTrend(String keyword, String host, String title, String festivalStartDate);



}
