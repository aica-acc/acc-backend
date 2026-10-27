package com.assistant.acc.service.report;

import org.springframework.beans.factory.annotation.Value;
import com.assistant.acc.domain.project.ProposalMetadata;
import com.assistant.acc.mapper.project.ProposalMetadataMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.assistant.acc.service.image.ImageService;
import com.assistant.acc.dto.image.PosterElementDTO;
import java.util.List;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ProposalMetadataMapper proposalMetadataMapper;
    private final RestTemplate restTemplate;
    private final ImageService imageService;

    // AI 서버 기본 주소
    @Value("${python.api.url:http://localhost:5000}")
    private String pythonApiUrl;

    @Override
    public String generateArticle(Integer projectNo) {
        return callAiServer(projectNo, "press", "/article");
    }

    @Override
    public String generateNotice(Integer projectNo) {
        return callAiServer(projectNo, "notice", "/notice");
    }

    @Override
    public String generateSns(Integer projectNo) {
        return callAiServer(projectNo, "sns", "/sns");
    }

    @Override
    public String generatePackage(Integer projectNo) {
        return callAiServer(projectNo, "package", "/package");
    }

    // 🔥 공통 메서드: DB 조회 + AI 요청
    private String callAiServer(Integer projectNo, String aiReportType, String endpoint) {
        // 1. DB 데이터 조회
        Map<String, String> metadataMap = new HashMap<>();
        try {
            ProposalMetadata meta = proposalMetadataMapper.findByPNo(projectNo);
            if (meta != null) {
                metadataMap.put("title", meta.getTitle());
                // 날짜 포맷팅
                String dateStr = "";
                if (meta.getFestivalStartDate() != null && meta.getFestivalEndDate() != null) {
                    dateStr = meta.getFestivalStartDate() + " ~ " + meta.getFestivalEndDate();
                }
                metadataMap.put("date", dateStr);
                metadataMap.put("location", meta.getLocation() != null ? meta.getLocation() : "");
                metadataMap.put("host", meta.getHost() != null ? meta.getHost() : "");
                metadataMap.put("programs", meta.getProgramName() != null ? meta.getProgramName() : "");
                metadataMap.put("concept", meta.getConceptDescription() != null ? meta.getConceptDescription() : "");
                metadataMap.put("contact", "문화관광과 (000-0000-0000)");
            } else {
                log.warn("⚠️ DB 데이터 없음 (pNo={}). 더미 데이터 사용.", projectNo);
                metadataMap.put("title", "테스트 축제");
                metadataMap.put("date", "2025.01.01");
                metadataMap.put("location", "서울");
                metadataMap.put("host", "테스트 주최");
                metadataMap.put("programs", "테스트 프로그램");
                metadataMap.put("concept", "테스트 컨셉");
                metadataMap.put("contact", "010-1234-5678");
            }

            // ✅ (2) [추가] 이미지 정보 가져오기 (ImageService 활용)
            List<PosterElementDTO> images = imageService.getProjectImages(projectNo);

            // 포스터 찾기
            String posterUrl = images.stream()
                    .filter(img -> "poster".equals(img.getAssetType()))
                    .findFirst()
                    .map(PosterElementDTO::getFileUrl)
                    .orElse("poster_main.jpg"); // 없으면 기본값

            // 마스코트 찾기 (필요하다면)
            String mascotUrl = images.stream()
                    .filter(img -> "mascot".equals(img.getAssetType()))
                    .findFirst()
                    .map(PosterElementDTO::getFileUrl)
                    .orElse("");

            // 메타데이터에 추가해서 AI로 보냄
            metadataMap.put("poster_image", posterUrl);
            metadataMap.put("mascot_image", mascotUrl);

        } catch (Exception e) {
            log.error("⚠️ DB 조회 중 오류: {}", e.getMessage());
        }



        // 2. AI 서버 요청 준비
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("report_type", aiReportType); // AI 모델 검증용 (Pydantic)
        requestBody.put("metadata", metadataMap);

        try {
            String url = pythonApiUrl + "/report" + endpoint;
            log.info("📤 AI 요청 전송: {} (type={})", url, aiReportType);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            Map<String, Object> body = response.getBody();

            if (body != null && body.containsKey("content")) {
                Object content = body.get("content");
                return content != null ? content.toString() : "";
            }
            return "AI 응답 없음";

        } catch (Exception e) {
            log.error("❌ AI 통신 오류 ({})", endpoint, e);
            throw new RuntimeException("AI 서버 연결 실패: " + e.getMessage());
        }
    }
}