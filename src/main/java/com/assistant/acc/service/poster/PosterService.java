package com.assistant.acc.service.poster;

import com.assistant.acc.domain.project.Project;
import com.assistant.acc.domain.project.ProposalMetadata;
import com.assistant.acc.domain.project.UserInput;
import com.assistant.acc.dto.poster.PosterAnalysisResponse;
import com.assistant.acc.dto.poster.PosterSummary;
import com.assistant.acc.dto.poster.PosterStrategy;
import com.assistant.acc.mapper.project.ProjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class PosterService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ProjectMapper projectMapper;

    private static final String PYTHON_API_URL = "http://localhost:5000";

    public PosterService(RestTemplate restTemplate, ObjectMapper objectMapper, ProjectMapper projectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.projectMapper = projectMapper;
    }

    @Transactional
    public String analyze(MultipartFile file, String theme, String keywords, String title) throws IOException {

        System.out.println("분석 시작 (서비스)");

        // 1️⃣ 새 프로젝트 생성
        String currentMemberId = "M0000001"; // 임시 하드코딩
        Project newProject = new Project();
        newProject.setMemberNo(currentMemberId);
        projectMapper.insertProject(newProject);
        Integer newPNo = newProject.getProjectNo();

        System.out.println("새 프로젝트 생성 완료 (p_no: " + newPNo + ")");

        // 2️⃣ 사용자 초기 입력 저장
        UserInput input = new UserInput();
        input.setProjectNo(newPNo);
        input.setTheme(theme);
        input.setKeywords(keywords);
        input.setPName(title);
        projectMapper.insertInitialUserInput(input);

        System.out.println("사용자 초기 입력 저장 완료.");

        // 3️⃣ Python 서버 호출
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("theme", theme);
        body.add("keywords", keywords);
        body.add("title", title);
        body.add("file", new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename();
            }
        });

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        System.out.println("Java → Python API 호출 시작...");
        ResponseEntity<String> response = restTemplate.postForEntity(
                PYTHON_API_URL + "/poster/analyze",
                requestEntity,
                String.class
        );

        // 4️⃣ 응답 DTO 매핑 (JsonNode → DTO 자동 변환)
        PosterAnalysisResponse parsed = objectMapper.readValue(response.getBody(), PosterAnalysisResponse.class);

        if (!"success".equals(parsed.getStatus())) {
            throw new IOException("AI 분석 실패: " + parsed.getMessage());
        }

        // 5️⃣ DTO 변환 및 DB 저장
        PosterSummary summary = parsed.getAnalysis_summary();
        PosterStrategy strategy = parsed.getStrategy_report();

        ProposalMetadata metadata = convertToMetadata(summary, strategy, newPNo);
        projectMapper.insertProposalMetadata(metadata);

        System.out.println("Python 분석 결과 DB 저장 완료.");
        return response.getBody();
    }

    /**
     * PosterSummary + PosterStrategy → ProposalMetadata 변환
     */
    private ProposalMetadata convertToMetadata(PosterSummary summary, PosterStrategy strategy, Integer projectNo) {
        ProposalMetadata metadata = new ProposalMetadata();
        metadata.setProjectNo(projectNo);

        metadata.setTitle(summary.getTitle());
        metadata.setLocation(summary.getLocation());
        metadata.setHost(summary.getHost());
        metadata.setOrganizer(summary.getOrganizer());
        metadata.setTarget(summary.getTargetAudience());
        metadata.setContactInfo(summary.getContactInfo());
        metadata.setDirections(summary.getDirections());

        List<Date> parsedDates = parseDateRange(summary.getDate());
        metadata.setFestivalStartDate(parsedDates.get(0));
        metadata.setFestivalEndDate(parsedDates.get(1));

        metadata.setProgramName(summary.getPrograms() != null ? summary.getPrograms().toString() : "[]");
        metadata.setEventName(summary.getEvents() != null ? summary.getEvents().toString() : "[]");
        metadata.setVisualKeywords(summary.getVisualKeywords() != null ? summary.getVisualKeywords().toString() : "[]");

        metadata.setConceptDescription(strategy.getStrategy_text());
        metadata.setCreateAt(new Date());

        return metadata;
    }

    /**
     * 날짜 파싱 유틸
     */
    private List<Date> parseDateRange(String rawDateText) {
        List<Date> resultDates = new ArrayList<>();
        Date parsedStartDate = null;
        Date parsedEndDate = null;

        try {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy. MM. dd");
            String[] dates = rawDateText.split("~");
            String startDateString = dates[0].replaceAll("\\(.*?\\)", "").trim();
            parsedStartDate = formatter.parse(startDateString);

            if (dates.length > 1) {
                String endDateString = dates[1].replaceAll("\\(.*?\\)", "").trim();
                if (endDateString.indexOf('.') == endDateString.lastIndexOf('.')) {
                    String year = startDateString.substring(0, 4);
                    endDateString = year + ". " + endDateString;
                }
                parsedEndDate = formatter.parse(endDateString);
            } else {
                parsedEndDate = parsedStartDate;
            }

        } catch (ParseException e) {
            System.err.println("날짜 파싱 실패: " + rawDateText + " | " + e.getMessage());
        }

        resultDates.add(parsedStartDate);
        resultDates.add(parsedEndDate != null ? parsedEndDate : parsedStartDate);
        return resultDates;
    }
}
