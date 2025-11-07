package com.assistant.acc.mapper.project;

import com.assistant.acc.domain.project.Project;
import com.assistant.acc.domain.project.UserInput;
import org.apache.ibatis.annotations.Mapper;

/**
 * DB에 저장 된 축제 정보를 가져오는 계층
 */
@Mapper
public interface ProjectMapper {

    // 1. 새 프로젝트 생성 (p_no가 DTO로 반환됨)
    void insertProject(Project project);
    // 2. 사용자의 초기 입력을 저장
    void insertInitialUserInput(UserInput userInput);
    // 3. AI 분석 완료 후, JSON 결과들을 업데이트
    void updateAnalysissResults(UserInput userInput);
    
}
