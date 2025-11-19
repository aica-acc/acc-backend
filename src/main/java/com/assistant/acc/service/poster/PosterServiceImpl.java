package com.assistant.acc.service.poster;

import com.assistant.acc.domain.project.Project;
import com.assistant.acc.domain.project.ProposalMetadata;
import com.assistant.acc.dto.poster.PosterAnalysisResponse;
import com.assistant.acc.dto.poster.PosterStrategy;
import com.assistant.acc.dto.poster.PosterSummary;
import com.assistant.acc.service.project.ProjectService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.*;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Arrays;

@Service
public class PosterServiceImpl implements PosterService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ProjectService projectService;

    private static final String PYTHON_API_URL = "http://localhost:5000";

    // 생성자 변경
    public PosterServiceImpl(RestTemplate restTemplate, ObjectMapper objectMapper, ProjectService projectService) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.projectService = projectService;
    }

    @Override //인터페이스 메서드 구현
    public String analyze(MultipartFile file, String theme, String keywords, String title) throws IOException {

        System.out.println("분석시작 (PosterService)");

        // 1- ProjectService에 작업 위임
        String currentMemberId = "M000001";
        Project newProject = projectService.createProjectAndSaveInput(theme, keywords, title, currentMemberId);
        Integer newPNo = newProject.getProjectNo();

        System.out.println("프로젝트 생성 및 입력 저장 위임 완료 (PosterService)");

        // 2 - python 서버 호출
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

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

        System.out.println("Java → Python API 호출 시작 (PosterService)");
        ResponseEntity<String> response = restTemplate.postForEntity(
                PYTHON_API_URL + "/analyze/proposal",
                request,
                String.class);

        //3- 응답 DTO 매핑
        PosterAnalysisResponse parsed = objectMapper.readValue(response.getBody(), PosterAnalysisResponse.class);

        if (!"success".equals(parsed.getStatus())) {
            throw new IOException("AI 분석 실패:" + parsed.getMessage());
        }

        // 4- DTO 변환 및 DB 저장 위임
        PosterSummary summary = parsed.getAnalysis_summary();
        PosterStrategy strategy = parsed.getStrategy_report();

        //4-1 DTO 변환 -posterservice 책임
        ProposalMetadata metadata = convertToMetadata(summary, strategy, newPNo);

        //4-2 DB 저장 위임
        projectService.saveProposalMetadata(metadata);

        return response.getBody();
    }

    /**
     * DTO를 DB 엔티티로 변환 (private 헬퍼)
     */
    private ProposalMetadata convertToMetadata(PosterSummary summary, PosterStrategy strategy, Integer newPNo) {
        ProposalMetadata metadata = new ProposalMetadata();
        metadata.setProjectNo(newPNo);

        metadata.setTitle(summary.getTitle());
        metadata.setLocation(summary.getLocation());
        metadata.setHost(summary.getHost());
        metadata.setOrganizer(summary.getOrganizer());
        metadata.setTarget(summary.getTargetAudience());
        metadata.setContactInfo(summary.getContactInfo());
        metadata.setDirections(summary.getDirections());

        // 날짜 파싱 로직
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
     * 날짜 파싱 (private 헬퍼)
     */
    private List<Date> parseDateRange(String rawDateText) {
        // (null 방지를 위해 기본값 null 대신 빈 리스트와 오늘 날짜로 초기화)
        if (rawDateText == null || rawDateText.trim().isEmpty()) {
            System.err.println("날짜 파싱 실패: 원본 텍스트가 비어있습니다.");
            Date now = new Date();
            return Arrays.asList(now, now); // 즉시 오늘 날짜 반환
        }

        System.out.println("날짜 데이터 (원본): " + rawDateText);
        List<Date> resultDates = new ArrayList<>();
        Date parsedStartDate = null;
        Date parsedEndDate = null;

        try {
            // 1. 공백이 없는 패턴 (yyyy.MM.dd)
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd");
            String[] dates = rawDateText.split("~");

            // 3. 시작 날짜 정제
            String startDateString = dates[0]
                    .replaceAll("\\(.*?\\)", "") // (요일) 제거
                    .replaceAll("/.*", "")     // " / n일간" 제거
                    .trim()                   // 공백 제거
                    .replaceFirst("\\.$", "");  // 마지막 점 제거

            parsedStartDate = formatter.parse(startDateString);

            if (dates.length > 1) {
                // 5. 종료 날짜 정제
                String endDateString = dates[1]
                        .replaceAll("\\(.*?\\)", "")
                        .replaceAll("/.*", "")
                        .trim()
                        .replaceFirst("\\.$", ""); //  마지막 점 제거

                // 6. 연도 자동 추가 로직 (12.25 -> 2025.12.25)
                if (endDateString.indexOf('.') == endDateString.lastIndexOf('.')) {
                    String year = startDateString.substring(0, 4);
                    endDateString = year + "." + endDateString;
                }
                parsedEndDate = formatter.parse(endDateString);
            } else {
                parsedEndDate = parsedStartDate;
            }

        } catch (ParseException | ArrayIndexOutOfBoundsException e) {
            System.err.println("날짜 파싱 실패: " + rawDateText + " | " + e.getMessage());
        }
        //7 - DB 오류 방지 코드
        if (parsedStartDate == null) {
            parsedStartDate = new Date();
        }
        resultDates.add(parsedStartDate);
        resultDates.add(parsedEndDate != null ? parsedEndDate : parsedStartDate);
        return resultDates;
    }
    // ------------------------------------
    // 2단계 generatePrompt 메소드
    // ------------------------------------
    @Override
    public String generatePrompt(String jsonBody) throws IOException {
        System.out.println("2단계 프롬프트 생성 시작 (PosterService)");

        // 1. 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        //2. body설정 JSON 문자열을 그대로 Entity
        HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);

        //3. AI 서버 2단계 API 호출
        System.out.println("Java → Python API 2단계 (/generate-prompt) 호출...");
        ResponseEntity<String> response = restTemplate.postForEntity(
                PYTHON_API_URL + "/generate-prompt",
                request,
                String.class
        );
        // 오류처리
        System.out.println("Python 2단계 응답 수신 완료.");
        return response.getBody(); // Python의 응답(JSON)을 그대로 반환
    }
    // ------------------------------------
    // 3단계 createImage 메소드
    // ------------------------------------
    @Override
    public String createImage(String jsonBody) throws IOException {
        System.out.println("3단계 이미지 생성 시작 (PosterService)");

        // 1. 헤더 설정(json)
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 2. body 설정 (postman에서 받은 json)
        HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);

        //3. AI 서버 3단계 API 호출
        System.out.println("Java → Python API 3단계 (/create-image) 호출...");
        ResponseEntity<String> response = restTemplate.postForEntity(
                PYTHON_API_URL + "/create-image",
                request,
                String.class
        );
        System.out.println("Python 3단계 응답 수신 완료.");
        return response.getBody();
    }
}
