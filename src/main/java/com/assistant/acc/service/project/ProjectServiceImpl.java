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

        // 2) Python 서버 호출 준비
        System.out.println("[Service - Step 2] Python 요청 준비 중...");
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
                    .replaceAll("/.*", "") // " / n일간" 제거
                    .trim() // 공백 제거
                    .replaceFirst("\\.$", ""); // 마지막 점 제거

            parsedStartDate = formatter.parse(startDateString);

            if (dates.length > 1) {
                // 5. 종료 날짜 정제
                String endDateString = dates[1]
                        .replaceAll("\\(.*?\\)", "")
                        .replaceAll("/.*", "")
                        .trim()
                        .replaceFirst("\\.$", ""); // 마지막 점 제거

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
        // 7 - DB 오류 방지 코드
        if (parsedStartDate == null) {
            parsedStartDate = new Date();
        }
        resultDates.add(parsedStartDate);
        resultDates.add(parsedEndDate != null ? parsedEndDate : parsedStartDate);
        return resultDates;
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
        Integer pNo = projectMapper.selectLatestProjectNo(m_no);
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
            log.error("❌ Python 지역 트렌드 분석 실패", e);
            return new RegionTrendResponseDTO(); // 빈 껍데기 반환
        }
    }

}
