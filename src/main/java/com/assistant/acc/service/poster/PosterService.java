package com.assistant.acc.service.poster;

import java.io.IOException;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.assistant.acc.domain.project.Project;
import com.assistant.acc.domain.project.UserInput;
import com.assistant.acc.mapper.project.ProjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class PosterService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper; 
    private final ProjectMapper projectMapper; 

    private final String PYTHON_API_URL = "http://localhost:5000";

    // 생성자 수정
    public PosterService(RestTemplate restTemplate, ObjectMapper objectMapper, ProjectMapper projectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.projectMapper = projectMapper;
    }

    /**
     * Python /analyze API를 호출하고, 전후 과정을 DB에 기록합니다.
     */
    @Transactional
    public String analyze(MultipartFile file, String theme, String keywords, String title) throws IOException {

        // =======================================================
        // 1: DB - 새 프로젝트 생성
        // =======================================================
        // TODO: (추후) Spring Security에서 실제 로그인된 사용자 m_no 가져오기
        String currentMemberId = "M0000001"; // (임시 하드코딩 - '테이블설계.csv'의 관리자 ID)

        Project newProject = new Project();
        newProject.setMemberNo(currentMemberId);

        projectMapper.insertProject(newProject); // 이 호출 후 newProject.getPNo()에 PK값이 채워짐
        Integer newPNo = newProject.getProjectNo();
        System.out.println("Java - DB: 새 프로젝트 생성 완료 (p_no: " + newPNo + ")");

        // =======================================================
        // 2: DB - 사용자 초기 입력 저장
        // =======================================================
        UserInput initialInput = new UserInput();
        initialInput.setProjectNo(newPNo);
        initialInput.setTheme(theme);
        initialInput.setKeywords(keywords);
        initialInput.setPName(title);

        projectMapper.insertInitialUserInput(initialInput);
        System.out.println("Java - DB: 사용자 초기 입력 저장 완료.");

        // =======================================================
        // 3: Python - AI 서버 호출
        // =======================================================
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("theme", theme);
        body.add("keywords", keywords);
        body.add("title", title);
        ByteArrayResource fileResource = new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename();
            }
        };
        body.add("file", fileResource);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        System.out.println("Java -> Python (" + PYTHON_API_URL + "/analyze) API 호출 시작...");
        ResponseEntity<String> response = restTemplate.postForEntity(
                PYTHON_API_URL + "/analyze",
                requestEntity,
                String.class
        );
        String pythonResponse = response.getBody();
        System.out.println("Java <- Python 통신 완료.");

        // =======================================================
        // 4: DB - Python 결과 파싱 및 DB 업데이트
        // =======================================================
        JsonNode root = objectMapper.readTree(pythonResponse);

        if (root.path("status").asText().equals("success")) {
            String analysisSummaryJson = root.path("analysis_summary").toString();
            String trendReportJson = root.path("poster_trend_report").toString();
            String strategyReportJson = root.path("strategy_report").toString();

            UserInput resultInput = new UserInput();
            resultInput.setProjectNo(newPNo); // 동일한 p_no로 지정
            resultInput.setAnalysisSummary(analysisSummaryJson);
            resultInput.setPosterTrendReport(trendReportJson);
            resultInput.setStrategyReport(strategyReportJson);

            projectMapper.insertAnalysissResults(resultInput);
            System.out.println("Java - DB: Python 분석 결과 업데이트 완료.");
        } else {
            // Python 서버가 'status: error'를 반환한 경우
            throw new IOException("Python AI 서버가 분석에 실패했습니다: " + root.path("message").asText());
        }

        // =======================================================
        //  5: React - 최종 결과 반환
        // =======================================================
        return pythonResponse;
    }
}