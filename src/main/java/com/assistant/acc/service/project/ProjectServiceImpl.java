package com.assistant.acc.service.project;

import com.assistant.acc.security.CurrentMember;
import org.springframework.beans.factory.annotation.Value;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.assistant.acc.dto.project.RegionTrendResponseDTO;
import org.apache.catalina.User;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
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

    @Value("${python.api.url:http://localhost:5000}")
    private String pythonApiUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ProjectMapper projectMapper;

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
        newProject.setMemberNo(memeberId);
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
        Integer latestPno = projectMapper.selectLatestProjectNo(CurrentMember.no());
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

        System.out.println("\n========================================");
        System.out.println("[Service] analyzeProposal 시작");
        System.out.println("========================================");

        // 1) 새로운 Project 생성
        System.out.println("[Service - Step 1] 프로젝트 생성 시작...");
        String memberId = CurrentMember.no();
        Project project = createProjectAndSaveInput(theme, keywords, title, memberId);
        Integer pNo = project.getProjectNo();
        System.out.println("[Service - Step 1] ✅ 프로젝트 생성 완료, pNo = " + pNo);

        return analyzeProposal(pNo, file.getBytes(), file.getOriginalFilename(), theme, keywords, title);
    }

    @Override
    public ProposalMetadata analyzeProposal(int pNo, byte[] file, String filename, String theme, String keywords,
            String title) throws IOException {
        // 2) Python 서버 호출 준비
        System.out.println("[Service - Step 2] Python 요청 준비 중...");
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(file) {
            @Override
            public String getFilename() {
                return filename;
            }
        });
        body.add("theme", theme);
        body.add("keywords", keywords);
        body.add("title", title);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
        System.out.println("[Service - Step 2] ✅ 요청 데이터 준비 완료");

        // 2-2) Python 분석 요청
        System.out.println("[Service - Step 3] Python 서버 호출 시작...");
        System.out.println("  URL: " + pythonApiUrl + "/analyze/proposal");

        ResponseEntity<String> response = restTemplate.postForEntity(
                pythonApiUrl + "/analyze/proposal",
                request,
                String.class);

        System.out.println("[Service - Step 3] ✅ Python 응답 수신 성공!");
        System.out.println("  Status Code: " + response.getStatusCode());
        System.out.println("  Response Body:");
        System.out.println(response.getBody());

        // 3) Python 응답을 DTO로 변환
        ProposalAnalyzeResponse parsed = objectMapper.readValue(response.getBody(), ProposalAnalyzeResponse.class);

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
                analysis.getPrograms() != null ? analysis.getPrograms().toString() : "[]");
        metadata.setEventName(
                analysis.getEvents() != null ? analysis.getEvents().toString() : "[]");
        metadata.setVisualKeywords(
                analysis.getVisualKeywords() != null ? analysis.getVisualKeywords().toString() : "[]");

        metadata.setCreateAt(new Date());

        // 5) 저장 시도 (❗ try/catch 추가)
        System.out.println("[Service - Step 6] DB 저장 시작...");
        try {
            saveProposalMetadata(metadata);
            System.out.println("[Service - Step 6] ✅ DB 저장 성공!");
        } catch (Exception e) {
            System.err.println("[Service - Step 6] ❌ DB 저장 실패!");
            System.err.println("  에러 타입: " + e.getClass().getName());
            System.err.println("  에러 메시지: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }

        System.out.println("[Service] ✅ analyzeProposal 완료!");
        System.out.println("========================================\n");
        return metadata;
    }

    /**
     * 날짜 파싱 (private 헬퍼)
     */
    // [연도 구분자]? 월 구분자 일. 연도는 4자리 또는 '21 형태. 구분자는 . - / 또는 년·월
    private static final java.util.regex.Pattern DATE_PART = java.util.regex.Pattern.compile(
            "(?:(\\d{4}|['’‘]\\d{2})\\s*[.\\-/년]\\s*)?(\\d{1,2})\\s*[.\\-/월]\\s*(\\d{1,2})(?!\\d)\\s*일?");
    private static final java.util.regex.Pattern YEAR = java.util.regex.Pattern.compile("(?<!\\d)(19|20)\\d{2}(?!\\d)");

    /**
     * 날짜 파싱 (private 헬퍼)
     * AI 가 돌려주는 형식이 일정하지 않아 여러 표기를 받는다.
     *  "2023.05.19 ~ 2023.05.21", "2023년 5월 19일(금) ~ 5월 21일(일), 3일간",
     *  "’21.10.1.(금) ~ 10.2.(토)", "2025.12.28 ~ 1.3"(해 넘김) 등.
     * 첫 날짜를 시작일, 마지막 날짜를 종료일로 쓰고, 종료일에 연도가 없으면 시작일 연도를 따른다.
     * 날짜를 하나도 찾지 못하면 DB 오류 방지를 위해 오늘 날짜로 대체한다.
     */
    private List<Date> parseDateRange(String rawDateText) {
        System.out.println("날짜 데이터 (원본): " + rawDateText);
        java.time.LocalDate[] range = rawDateText == null ? null : parseDateParts(rawDateText);
        if (range == null) {
            System.err.println("날짜 파싱 실패, 오늘 날짜로 대체: " + rawDateText);
            Date now = new Date();
            return Arrays.asList(now, now);
        }
        return Arrays.asList(java.sql.Date.valueOf(range[0]), java.sql.Date.valueOf(range[1]));
    }

    private java.time.LocalDate[] parseDateParts(String raw) {
        java.util.regex.Matcher m = DATE_PART.matcher(raw);
        List<int[]> parts = new ArrayList<>(); // {연도(없으면 -1), 월, 일}
        while (m.find()) {
            int year = -1;
            if (m.group(1) != null) {
                String digits = m.group(1).replaceAll("[^0-9]", "");
                year = digits.length() == 2 ? 2000 + Integer.parseInt(digits) : Integer.parseInt(digits);
            }
            int month = Integer.parseInt(m.group(2));
            int day = Integer.parseInt(m.group(3));
            if (month >= 1 && month <= 12 && day >= 1 && day <= 31) {
                parts.add(new int[] { year, month, day });
            }
        }
        if (parts.isEmpty()) {
            return null;
        }
        int[] start = parts.get(0);
        int[] end = parts.get(parts.size() - 1);

        int startYear = start[0];
        if (startYear < 0) {
            java.util.regex.Matcher ym = YEAR.matcher(raw);
            if (!ym.find()) {
                return null;
            }
            startYear = Integer.parseInt(ym.group());
        }
        int endYear = end[0] >= 0 ? end[0] : startYear;
        // 종료일 연도가 생략됐는데 시작일보다 앞서면 해를 넘긴 축제 (12.28 ~ 1.3)
        if (end[0] < 0 && (end[1] < start[1] || (end[1] == start[1] && end[2] < start[2]))) {
            endYear = startYear + 1;
        }
        try {
            return new java.time.LocalDate[] {
                    java.time.LocalDate.of(startYear, start[1], start[2]),
                    java.time.LocalDate.of(endYear, end[1], end[2]) };
        } catch (java.time.DateTimeException e) {
            return null; // 2월 30일 같은 잘못된 날짜
        }
    }

    @Override
    public Map<String, Object> analyzeTotalTrend(String keyword, String title, String festivalStartDate)
            throws IOException {

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
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(formData, headers);

            // 4️⃣ Python FastAPI 호출
            String pythonUrl = pythonApiUrl + "/analyze/total_trend";

            Map<String, Object> result = restTemplate.postForObject(
                    pythonUrl,
                    requestEntity,
                    Map.class);

            log.info("✔ Python 응답 수신: {}", result);
            return result;

        } catch (Exception e) {
            log.error("❌ Python 트렌드 분석 실패", e);

            return Map.of(
                    "error", "Python 서버 요청 실패",
                    "details", e.getMessage());
        }
    }

    @Override
    public RegionTrendResponseDTO analyzeRegionTrend(String m_no, String festivalStartDate) {
        return analyzeRegionTrendFor(projectMapper.selectLatestProjectNo(m_no), festivalStartDate);
    }

    @Override
    public RegionTrendResponseDTO analyzeRegionTrendFor(int pNo, String festivalStartDate) {
        ProposalMetadata meta = projectMapper.selectProposalMetadata(pNo);

        log.info("📡 [ServiceImpl] 지역 트렌드 요청: title={}, host={}", meta.getTitle(), meta.getHost());

        try {
            // =========================================================
            // [STEP 1] 분석에 필요한 데이터(키워드, 날짜) 확보
            // =========================================================
            // A. 키워드 가져오기 (UserInput 테이블 조회)
            UserInput userInput = projectMapper.selectUserInput(pNo);
            String keyword = (userInput != null) ? userInput.getKeywords() : meta.getTitle(); //

            String host = meta.getHost();
            String title = meta.getTitle();

            // B. 날짜 처리 (DB에 있는 축제 시작일 사용)
            if (festivalStartDate == null || festivalStartDate.isEmpty()) {
                if (meta.getFestivalStartDate() != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    festivalStartDate = sdf.format(meta.getFestivalStartDate());
                } else {
                    // DB에도 없으면 오늘 날짜
                    festivalStartDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
                }
            }

            // =========================================================
            // [STEP 2] 파이썬 AI 서버로 요청 보내기
            // =========================================================
            // 1. 파이썬으로 보낼 데이터 (Form Data)
            MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();
            formData.add("keyword", keyword);
            formData.add("host", host);
            // 주최가 비어 있으면('정보 없음') AI 가 장소로 지역(시·군)을 추정한다
            formData.add("location", meta.getLocation() == null ? "" : meta.getLocation());
            formData.add("title", title);
            formData.add("festivalStartDate", festivalStartDate);

            // 2. 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            // 3. 요청 생성
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(formData, headers);

            // 4. 파이썬 서버 호출
            String pythonUrl = pythonApiUrl + "/analyze/region_trend";

            ResponseEntity<RegionTrendResponseDTO> response = restTemplate.postForEntity(
                    pythonUrl,
                    requestEntity,
                    RegionTrendResponseDTO.class);
            log.info("✔ Python 지역 트렌드 분석 성공");
            return response.getBody();

        } catch (Exception e) {
            // 예전에는 빈 결과를 200 으로 돌려줘 크레딧도 환불되지 않았다 (#31). 실패는 실패로
            log.error("❌ Python 지역 트렌드 분석 실패", e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "지역 트렌드를 분석하지 못했습니다. 잠시 후 다시 시도해 주세요.");
        }
    }

}
