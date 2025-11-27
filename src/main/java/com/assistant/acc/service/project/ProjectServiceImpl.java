package com.assistant.acc.service.project;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

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
import com.assistant.acc.domain.project.ProposalMetadata;
import com.assistant.acc.domain.project.UserInput;
import com.assistant.acc.dto.project.ProposalAnalyze;
import com.assistant.acc.dto.project.ProposalAnalyzeResponse;
import com.assistant.acc.mapper.project.ProjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;


@Slf4j
@Service
public class ProjectServiceImpl implements ProjectService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ProjectMapper projectMapper;

    // 고정 회원 ID
    private static final String DEFAULT_MEMBER_NO = "M000001";
            
    public ProjectServiceImpl(RestTemplate restTemplate, ProjectMapper projectMapper, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
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
          
    @Override
    public ProposalMetadata getLatestProposalMetadata() {

        // 1) 최신 프로젝트 번호 조회
        Integer latestPno = projectMapper.selectLatestProjectNo(DEFAULT_MEMBER_NO);
        System.out.println("latestPno: " + latestPno);
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

    @Override
    @Transactional
    public ProposalMetadata analyzeProposal(
            MultipartFile file, String theme, String keywords, String title) throws IOException {

        // 1) 새로운 Project 생성
        String memberId = DEFAULT_MEMBER_NO;
        Project project = createProjectAndSaveInput(theme, keywords, title, memberId);
        Integer pNo = project.getProjectNo();

        System.out.println("📌 새 프로젝트 생성 완료, pNo = " + pNo);

        // 2) Python 서버 호출 준비
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename();
            }
        });
        body.add("theme", theme);
        body.add("keywords", keywords);
        body.add("title", title);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

        // 2-2) Python 분석 요청
        
        ResponseEntity<String> response = restTemplate.postForEntity(
                "http://localhost:5000/analyze/proposal",
                request,
                String.class
        );

        System.out.println("📥 Python Response Body:");
        System.out.println(response.getBody());

        // 3) Python 응답을 DTO로 변환
        ProposalAnalyzeResponse parsed =
                objectMapper.readValue(response.getBody(), ProposalAnalyzeResponse.class);

        if (!"success".equals(parsed.getStatus())) {
            throw new IOException("AI 분석 실패");
        }

        ProposalAnalyze analysis = parsed.getAnalysis();

        // 4) DTO → ProposalMetadata 변환
        ProposalMetadata metadata = new ProposalMetadata();
        metadata.setProjectNo(pNo);

        metadata.setTitle(analysis.getTitle());
        metadata.setLocation(analysis.getLocation());
        metadata.setHost(analysis.getHost());
        metadata.setOrganizer(analysis.getOrganizer());
        metadata.setTarget(analysis.getTargetAudience());
        metadata.setContactInfo(analysis.getContactInfo());
        metadata.setDirections(analysis.getDirections());
        metadata.setConceptDescription(analysis.getSummary());

        // 날짜 파싱
        List<Date> parsedDates = parseDateRange(analysis.getDate());
        metadata.setFestivalStartDate(parsedDates.get(0));
        metadata.setFestivalEndDate(parsedDates.get(1));

        metadata.setProgramName(
                analysis.getPrograms() != null ? analysis.getPrograms().toString() : "[]"
        );
        metadata.setEventName(
                analysis.getEvents() != null ? analysis.getEvents().toString() : "[]"
        );
        metadata.setVisualKeywords(
                analysis.getVisualKeywords() != null ? analysis.getVisualKeywords().toString() : "[]"
        );

        metadata.setCreateAt(new Date());

        // 5) 저장 시도 (❗ try/catch 추가)
        try {
            saveProposalMetadata(metadata);
            System.out.println("📌 기획서 분석 결과 저장 완료");
        } catch (Exception e) {
            System.out.println("❌ ProposalMetadata 저장 실패");
            e.printStackTrace(); // 실제 오류 콘솔에 출력
            throw e; // 컨트롤러로 예외 전달 → 프론트에 500 반환
        }

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

    @Override
    public Map<String, Object> analyzeTotalTrend(String keyword, String title, String festivalStartDate) throws IOException {

        log.info("📡 [ServiceImpl] Python 트렌드 요청: keyword={}, title={}", keyword, title);

        try {
            // 1️⃣ FormData 생성
            MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();
            formData.add("keyword", keyword);
            formData.add("title", title);
            formData.add("festivalStartDate", festivalStartDate);

            // 2️⃣ 헤더 설정 (multipart/form-data)
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            // 3️⃣ HttpEntity 생성
            HttpEntity<MultiValueMap<String, Object>> requestEntity =
                    new HttpEntity<>(formData, headers);

            // 4️⃣ Python FastAPI 호출
            String pythonUrl = "http://localhost:5000/analyze/total_trend";

            Map<String, Object> result = restTemplate.postForObject(
                    pythonUrl,
                    requestEntity,
                    Map.class
            );

            log.info("✔ Python 응답 수신: {}", result);
            return result;

        } catch (Exception e) {
            log.error("❌ Python 트렌드 분석 실패", e);

            return Map.of(
                    "error", "Python 서버 요청 실패",
                    "details", e.getMessage()
            );
        }
    }

}    
