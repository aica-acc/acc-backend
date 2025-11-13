package com.assistant.acc.service.project;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.assistant.acc.domain.project.Project;
import com.assistant.acc.domain.project.ProposalMetadata;
import com.assistant.acc.domain.project.UserInput;
import com.assistant.acc.mapper.project.ProjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ProjectServiceImpl implements ProjectService {

    private final ProjectMapper projectMapper;

    // 고정 회원 ID
    private static final String DEFAULT_MEMBER_NO = "M000001";
            
    public ProjectServiceImpl(ProjectMapper projectMapper) {
        this.projectMapper = projectMapper;
    }

    /**
     * 'PosterService'에서 이동해 온 '프로젝트 생성 및 입력 저장' 로직
     */ 
    @Override
    @Transactional
    public Project createProjectAndSaveInput(String theme, String keywords, String title, String memeberId) {
        Project newProject = new Project();
        newProject.setMemberNo("M000001");
        projectMapper.insertProject(newProject);
        Integer newPNo = newProject.getProjectNo();

        System.out.println("새 프로젝트 생성 완료 (ProjectService - p_no: " + newPNo + ")");

        // 사용자 초기 입력 저장
        UserInput input = new UserInput();
        input.setProjectNo(newPNo);
        input.setTheme(theme);
        input.setKeywords(keywords);
        input.setPName(title);
        projectMapper.insertInitialUserInput(input);

        System.out.println("사용자 초기 입력 저장 완료 (ProjectService)");

        return newProject;
    }

    /**
     * 'PosterService'에서 이동해 온 '메타데이터 저장' 로직
     */
    @Override
    @Transactional
    public void saveProposalMetadata(ProposalMetadata metadata) {
        projectMapper.insertProposalMetadata(metadata);
        System.out.println("Python 분석 결과 DB 저장 완료 (ProjectService)");
    }


    /**
     * 메타데이터 저장 불러오는 로직
     */
    // @Override
    // public ProposalMetadata getLatestProposalMetadata() {

    //     // 1) 최신 프로젝트 번호 조회
    //     Integer latestPno = projectMapper.selectLatestProjectNo(DEFAULT_MEMBER_NO);
    //     if (latestPno == null) {
    //         return null; // 생성된 프로젝트 없음
    //     }

    //     // 2) 해당 프로젝트의 기획서 메타데이터 조회
    //     return projectMapper.selectProposalMetadata(latestPno);
    // }

    @Override
    public ProposalMetadata getLatestProposalMetadata() {

        // 1) 최신 프로젝트 번호 조회
        Integer latestPno = projectMapper.selectLatestProjectNo(DEFAULT_MEMBER_NO);
        if (latestPno == null) {
            return null;
        }

        // 2) 해당 프로젝트의 기획서 메타데이터 조회
        ProposalMetadata metadata = projectMapper.selectProposalMetadata(latestPno);

        // 🔥 여기서 metadata 로그 찍기 (프론트로 보내기 직전)
        try {
            System.out.println("🔥 [BACKEND] GET metadata result:");
            System.out.println(new ObjectMapper().writeValueAsString(metadata));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return metadata;
    }

}
