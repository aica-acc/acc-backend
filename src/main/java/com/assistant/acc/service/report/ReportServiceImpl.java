package com.assistant.acc.service.report;

import org.springframework.beans.factory.annotation.Value;
import com.assistant.acc.domain.project.ProposalMetadata;
import com.assistant.acc.mapper.project.ProposalMetadataMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import com.assistant.acc.service.image.ImageService;
import com.assistant.acc.dto.image.PosterElementDTO;
import com.assistant.acc.security.CurrentMember;
import com.assistant.acc.security.DemoBlockedException;
import com.assistant.acc.security.ProjectAccess;
import com.assistant.acc.service.credit.CreditService;
import com.assistant.acc.service.credit.CreditService.Charge;
import org.springframework.jdbc.core.JdbcTemplate;
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
    private final JdbcTemplate jdbc;
    private final CreditService creditService;
    private final ProjectAccess projectAccess;

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

    /**
     * 리포트는 프로젝트·종류마다 처음 한 번만 만들어 저장한다 (예전에는 화면을 열 때마다 AI 를 4번 불렀다).
     * 데모(test) 계정은 저장된 것만 본다.
     */
    private String callAiServer(Integer projectNo, String aiReportType, String endpoint) {
        projectAccess.check(projectNo);
        String cached = jdbc.query("SELECT content FROM report_cache WHERE p_no = ? AND report_type = ?",
                rs -> rs.next() ? rs.getString(1) : null, projectNo, aiReportType);
        if (cached != null) {
            return cached;
        }
        if (CurrentMember.isTest()) {
            throw new DemoBlockedException("데모에서는 미리 만들어 둔 리포트만 볼 수 있습니다.");
        }
        Charge charge = creditService.charge(CurrentMember.no(), CurrentMember.role(),
                creditService.cost("report"), "리포트", projectNo);
        String content;
        try {
            content = generate(projectNo, aiReportType, endpoint);
        } catch (RuntimeException e) {
            charge.refund();
            throw e;
        }
        if (content == null || content.isBlank() || "AI 응답 없음".equals(content)) {
            charge.refund();
            return content;
        }
        jdbc.update("INSERT INTO report_cache (p_no, report_type, content) VALUES (?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE content = VALUES(content), created_at = NOW()", projectNo, aiReportType, content);
        return content;
    }

    // 🔥 공통 메서드: DB 조회 + AI 요청
    private String generate(Integer projectNo, String aiReportType, String endpoint) {
        // 1. DB 데이터 조회 (기획서 정보가 없으면 가짜 값으로 채우지 않고 중단)
        ProposalMetadata meta = proposalMetadataMapper.findByPNo(projectNo);
        if (meta == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "기획서 정보가 없어 보고서를 만들 수 없습니다.");
        }
        Map<String, String> metadataMap = new HashMap<>();
        try {
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
            // 문의처는 기획서에 적힌 값만 쓴다 (없으면 비워 두고 화면에서 숨김)
            metadataMap.put("contact", meta.getContactInfo() != null ? meta.getContactInfo() : "");

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
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "보고서를 만들지 못했습니다. 잠시 후 다시 시도해 주세요.");
        }
    }
}