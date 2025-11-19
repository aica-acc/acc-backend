package com.assistant.acc.service.poster;

import com.assistant.acc.domain.project.Project;
import com.assistant.acc.domain.project.ProposalMetadata;
import com.assistant.acc.dto.image.ImageRegenerateResponseDTO;
import com.assistant.acc.dto.image.PosterElementDTO;
import com.assistant.acc.dto.poster.PosterAnalysisResponse;
import com.assistant.acc.dto.poster.PosterArchiveDTO;
import com.assistant.acc.dto.poster.PosterStrategy;
import com.assistant.acc.dto.poster.PosterSummary;
import com.assistant.acc.mapper.poster.PosterArchiveMapper;
import com.assistant.acc.service.project.ProjectService;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class PosterServiceImpl implements PosterService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ProjectService projectService;
    private final PosterArchiveMapper posterArchiveMapper; //

    private static final String PYTHON_API_URL = "http://localhost:5000";

    // 생성자 주입 (Mapper 포함)
    public PosterServiceImpl(RestTemplate restTemplate, ObjectMapper objectMapper, ProjectService projectService, PosterArchiveMapper posterArchiveMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.projectService = projectService;
        this.posterArchiveMapper = posterArchiveMapper;
    }

    // ==========================================================================
    // [NEW] 인터페이스 요구사항 구현 (프론트엔드 연동용)
    // ==========================================================================

    @Override
    public PosterArchiveDTO getPosterById(Integer filePathNo) {
        // DB에서 단일 포스터 조회
        return posterArchiveMapper.findById(filePathNo);
    }

    @Override
    public List<PosterElementDTO> getPosterPrompts(Integer projectNo) {
        // DB에서 프로젝트의 포스터 목록 조회
        return posterArchiveMapper.findPromptsByProjectNo(projectNo);
    }

    @Override
    public ImageRegenerateResponseDTO regeneratePoster(Integer filePathNo, String visualPrompt) throws IOException {
        System.out.println("🔄 [PosterService] 재생성 요청: " + filePathNo);

        // 1. 기존 정보 확인
        PosterArchiveDTO existing = posterArchiveMapper.findById(filePathNo);
        if (existing == null) {
            throw new IOException("해당 포스터를 찾을 수 없습니다. ID: " + filePathNo);
        }

        // 2. Python 요청 데이터 준비
        Map<String, Object> pythonRequest = new HashMap<>();

        // Python 모델(CreateImageRequest) 규격 맞춤
        Map<String, Object> analysisSummary = new HashMap<>();
        analysisSummary.put("title", "");

        Map<String, Object> selectedPrompt = new HashMap<>();
        selectedPrompt.put("style_name", existing.getStyleName() != null ? existing.getStyleName() : "Custom");
        selectedPrompt.put("visual_prompt", visualPrompt);
        selectedPrompt.put("width", 1024);
        selectedPrompt.put("height", 1792);
        selectedPrompt.put("suggested_text_style", "User Modified");
        selectedPrompt.put("text_content", Map.of());

        pythonRequest.put("analysis_summary", analysisSummary);
        pythonRequest.put("selected_prompt", selectedPrompt);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(pythonRequest, headers);

        // 3. Python 호출 (/create-image)
        ResponseEntity<String> response = restTemplate.postForEntity(
                PYTHON_API_URL + "/create-image",
                request,
                String.class
        );

        // 4. 결과 파싱
        Map<String, Object> result = objectMapper.readValue(response.getBody(), Map.class);
        String newImageUrl = (String) result.get("image_url");

        // 5. DB 업데이트
        posterArchiveMapper.updatePosterImage(filePathNo, newImageUrl, visualPrompt);

        return new ImageRegenerateResponseDTO(filePathNo, newImageUrl, visualPrompt, true, "success");
    }

    @Override
    public String generateDrafts(String jsonBody) throws IOException {
        // (구현 생략 또는 필요 시 추가)
        return "";
    }

    // ==========================================================================
    // 기존 기능 (유지)
    // ==========================================================================

    @Override
    @Transactional
    public String analyze(MultipartFile file, String theme, String keywords, String title) throws IOException {
        System.out.println("분석시작 (PosterService)");
        String currentMemberId = "M000001";
        Project newProject = projectService.createProjectAndSaveInput(theme, keywords, title, currentMemberId);
        Integer newPNo = newProject.getProjectNo();

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("theme", theme);
        body.add("keywords", keywords);
        body.add("title", title);
        body.add("file", new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() { return file.getOriginalFilename(); }
        });

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

        System.out.println("Java → Python API 호출 시작 (/analyze/proposal)");
        ResponseEntity<String> response = restTemplate.postForEntity(
                PYTHON_API_URL + "/analyze/proposal",
                request,
                String.class);

        PosterAnalysisResponse parsed = objectMapper.readValue(response.getBody(), PosterAnalysisResponse.class);
        if (!"success".equals(parsed.getStatus())) {
            throw new IOException("AI 분석 실패:" + parsed.getMessage());
        }

        PosterSummary summary = parsed.getAnalysis_summary();
        PosterStrategy strategy = parsed.getStrategy_report();
        ProposalMetadata metadata = convertToMetadata(summary, strategy, newPNo);
        projectService.saveProposalMetadata(metadata);

        return response.getBody();
    }

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

    private List<Date> parseDateRange(String rawDateText) {
        if (rawDateText == null || rawDateText.trim().isEmpty()) {
            return Arrays.asList(new Date(), new Date());
        }
        List<Date> resultDates = new ArrayList<>();
        Date parsedStartDate = null;
        Date parsedEndDate = null;
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd");
            String[] dates = rawDateText.split("~");
            String startDateString = dates[0].replaceAll("\\(.*?\\)", "").replaceAll("/.*", "").trim().replaceFirst("\\.$", "");
            parsedStartDate = formatter.parse(startDateString);
            if (dates.length > 1) {
                String endDateString = dates[1].replaceAll("\\(.*?\\)", "").replaceAll("/.*", "").trim().replaceFirst("\\.$", "");
                if (endDateString.indexOf('.') == endDateString.lastIndexOf('.')) {
                    String year = startDateString.substring(0, 4);
                    endDateString = year + "." + endDateString;
                }
                parsedEndDate = formatter.parse(endDateString);
            } else {
                parsedEndDate = parsedStartDate;
            }
        } catch (ParseException | ArrayIndexOutOfBoundsException e) {
            System.err.println("날짜 파싱 실패: " + rawDateText);
        }
        if (parsedStartDate == null) parsedStartDate = new Date();
        resultDates.add(parsedStartDate);
        resultDates.add(parsedEndDate != null ? parsedEndDate : parsedStartDate);
        return resultDates;
    }

    @Override
    public String generatePrompt(String jsonBody) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(PYTHON_API_URL + "/generate-prompt", request, String.class);
        return response.getBody();
    }

    @Override
    public String createImage(String jsonBody) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(PYTHON_API_URL + "/create-image", request, String.class);
        return response.getBody();
    }
}