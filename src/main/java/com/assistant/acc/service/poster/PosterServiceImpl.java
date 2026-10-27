package com.assistant.acc.service.poster;

import org.springframework.beans.factory.annotation.Value;
import com.assistant.acc.domain.project.Project;
import com.assistant.acc.domain.project.ProposalMetadata;
import com.assistant.acc.dto.create.prompt.PosterPromptApiRequest;
import com.assistant.acc.dto.image.ImageRegenerateResponseDTO;
import com.assistant.acc.dto.image.PosterElementDTO;
import com.assistant.acc.dto.poster.*;
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
    private final PosterArchiveMapper posterArchiveMapper;

    // 파이썬 서버 주소 (포트 번호 확인! 아까 5000 혹은 5001로 하셨죠?)
    @Value("${python.api.url:http://localhost:5000}")
    private String pythonApiUrl;

    // 생성자 주입 (Mapper 포함)
    public PosterServiceImpl(RestTemplate restTemplate, ObjectMapper objectMapper, ProjectService projectService, PosterArchiveMapper posterArchiveMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.projectService = projectService;
        this.posterArchiveMapper = posterArchiveMapper;
    }

    // ... (getPosterById, getPosterPrompts 등 조회 메서드는 기존 그대로 유지) ...

    @Override
    public PosterArchiveDTO getPosterById(Integer filePathNo) {
        return posterArchiveMapper.findById(filePathNo);
    }

    @Override
    public List<PosterElementDTO> getPosterPrompts(Integer projectNo) {
        return posterArchiveMapper.findPromptsByProjectNo(projectNo);
    }

    // 🔥 [수정됨] AI에게 "글씨 수정해줘" 요청하는 깔끔한 버전
    @Override
    public ImageRegenerateResponseDTO regeneratePoster(Integer filePathNo, String visualPrompt) throws IOException {
        System.out.println("🔄 [PosterService] AI 수정 요청 (Gemini): " + filePathNo);

        // 1. 수정할 포스터 정보 가져오기
        PosterArchiveDTO existing = posterArchiveMapper.findById(filePathNo);
        if (existing == null) {
            throw new IOException("포스터를 찾을 수 없습니다. ID: " + filePathNo);
        }

        // 2. 넣어야 할 텍스트 정보(제목, 날짜, 장소) DB에서 가져오기
        String title = "축제 제목 없음";
        String date = "날짜 미정";
        String place = "장소 미정";

        // ProjectService를 통해 메타데이터 조회 (만약 메서드가 없다면 추가 필요)
        // 예시: projectService.getProposalMetadata(projectNo)
        try {
            // ProposalMetadata meta = projectService.getProposalMetadata(existing.getProjectNo());
            // if (meta != null) {
            //    title = meta.getTitle();
            //    SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd");
            //    date = sdf.format(meta.getFestivalStartDate()) + " - " + sdf.format(meta.getFestivalEndDate());
            //    place = meta.getLocation();
            // }

            // ⚠️ 지금은 테스트용으로 고정값 사용 (나중에 위 주석 풀어서 DB 연결하세요!)
            title = "거제 몽돌 축제";
            date = "2025.07.14 - 07.15";
            place = "학동 흑진주 몽돌해변";

        } catch (Exception e) {
            System.out.println("⚠️ 메타데이터 조회 실패, 기본값 사용");
        }

        // 3. 파이썬 서버로 보낼 데이터 준비 (Multipart 요청)
        // 파일(이미지)은 URL로 보내거나, 파일을 다운받아 보내야 함.
        // 여기서는 '이미지 URL'과 '텍스트 정보'를 같이 보냅니다.

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        // A. 이미지 파일 준비 (URL에서 다운로드해서 바로 전송)
        String currentImageUrl = existing.getFileUrl(); // "http://..." 또는 "/static/..."
        if (currentImageUrl != null && !currentImageUrl.startsWith("http")) {
            // 로컬 경로라면 http://localhost:8080... 형태로 만들어줘야 파이썬이 다운 가능할 수도 있음
            // 혹은 파일을 직접 읽어서 바이트로 전송 (이게 더 확실함)
            // body.add("file", new FileSystemResource("src/main/resources" + currentImageUrl));
        }

        // 💡 팁: 파이썬 쪽에서 URL만 줘도 다운받게 만들면 편함.
        // 일단은 "이미지 파일"을 직접 보내는 방식(Form Data)으로 가정하고 작성합니다.
        // (기존 analyze 메서드 참고)

        body.add("image_url", existing.getFileUrl()); // 파이썬이 다운받도록 URL 전달
        body.add("title", title);
        body.add("date", date);
        body.add("location", place);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

        // 4. 파이썬 호출 (아까 만든 /test/gemini-capability 경로 사용)
        // 나중에 실제 경로명으로 바꾸세요 (예: /api/edit-poster-ai)
        ResponseEntity<String> response = restTemplate.postForEntity(
                pythonApiUrl + "/create-image",
                request,
                String.class
        );

        // 5. 결과 받기 (파이썬이 준 완성된 이미지 URL)
        // 파이썬 응답 예시: { "status": "success", "result_image_url": "http://..." }
        Map<String, Object> result = objectMapper.readValue(response.getBody(), Map.class);

        // 만약 파이썬이 텍스트만 줬다면 에러 처리, 이미지 URL을 줬다면 그걸 씀
        String newImageUrl = "";
        if (result.containsKey("ai_response_text")) {
            // 이미지 생성 실패 시 (텍스트만 온 경우)
            System.out.println("⚠️ AI 응답(텍스트): " + result.get("ai_response_text"));
            // newImageUrl = existing.getFilePath(); // 원본 유지
        } else {
            // 성공 시 (이미지 경로가 왔다고 가정)
            // newImageUrl = (String) result.get("result_image_url");
        }

        // 6. DB 업데이트
        // posterArchiveMapper.updatePosterImage(filePathNo, newImageUrl, visualPrompt);

        return new ImageRegenerateResponseDTO(filePathNo, newImageUrl, visualPrompt, true, "success");
    }

    // ... (나머지 기존 메서드들: analyze, generateDrafts 등등 유지) ...
    @Override
    @Transactional
    public String analyze(MultipartFile file, String theme, String keywords, String title) throws IOException {
        // (기존 내용 생략 - 그대로 유지하세요!)
        return null;
    }

    public PosterPromptResponse generatePrompt(PosterPromptApiRequest requestDto) {
        // 1. 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 2. 요청 객체(DTO)를 Entity에 담기 (Spring이 알아서 JSON으로 바꿔줍니다)
        HttpEntity<PosterPromptApiRequest> requestEntity = new HttpEntity<>(requestDto, headers);

        // 3. Python 호출 (응답도 DTO 클래스를 지정하면 알아서 파싱해줍니다)
        ResponseEntity<PosterPromptResponse> response = restTemplate.postForEntity(
                pythonApiUrl + "/generate-prompt",
                requestEntity,
                PosterPromptResponse.class
        );

        return response.getBody();
    }

    @Override
    public String generateDrafts(String jsonBody) throws IOException { return null; }
}