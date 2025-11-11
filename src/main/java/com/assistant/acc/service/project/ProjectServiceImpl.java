package com.assistant.acc.service.project;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.assistant.acc.domain.project.Project;
import com.assistant.acc.domain.project.ProposalMetadata;
import com.assistant.acc.domain.project.UserInput;
import com.assistant.acc.mapper.project.ProjectMapper;

@Service
public class ProjectServiceImpl implements ProjectService {

    private final ProjectMapper projectMapper;

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
        newProject.setMemberNo("M0000001");
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
}
