package com.assistant.acc.controller.project;

import com.assistant.acc.domain.project.ProposalMetadata;
import com.assistant.acc.dto.project.RegionTrendResponseDTO;
import com.assistant.acc.security.CurrentMember;
import com.assistant.acc.service.credit.CreditService;
import com.assistant.acc.service.credit.CreditService.Charge;
import com.assistant.acc.service.job.GenerationJobService;
import com.assistant.acc.service.job.GenerationJobService.Job;
import com.assistant.acc.service.poster.PosterService;
import com.assistant.acc.service.project.ProjectDeleteService;
import com.assistant.acc.service.project.ProjectService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import java.util.LinkedHashMap;
import java.util.Map;
import java.io.IOException;
import java.io.UncheckedIOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/project")
public class ProjectController {

    private final ProjectService projectService;
    private final ProjectDeleteService projectDeleteService;
    private final CreditService creditService;
    private final GenerationJobService jobService;
    // 생성자 주입 방식 (권장)

    @Autowired
    public ProjectController(PosterService posterService, ProjectService projectService,
            ProjectDeleteService projectDeleteService, CreditService creditService, GenerationJobService jobService) {
        this.projectService = projectService;
        this.projectDeleteService = projectDeleteService;
        this.creditService = creditService;
        this.jobService = jobService;
    }

    /** 유료 분석 차감 (관리자는 0). 데모 계정은 DemoGuardInterceptor 가 먼저 막는다 */
    private Charge charge(String costKey, String what) {
        return creditService.charge(CurrentMember.no(), CurrentMember.role(), creditService.cost(costKey), what, null);
    }

    /** 프로젝트 삭제 (본인 프로젝트만). DB 기록과 생성 파일 폴더를 함께 지운다 */
    @DeleteMapping("/{pNo}")
    public ResponseEntity<Void> deleteProject(@PathVariable int pNo) {
        projectDeleteService.deleteProject(pNo);
        return ResponseEntity.noContent().build();
    }

    // POST 요청으로 파일 + 문자열 데이터 받기
    @PostMapping("/analyze/proposal")
    public ResponseEntity<ProposalMetadata> analyzeProposal(
            @RequestParam("file") MultipartFile file,
            @RequestParam("theme") String theme,
            @RequestParam("keywords") String keywords,
            @RequestParam("title") String title) {
        System.out.println("========================================");
        System.out.println("[Controller] /analyze/proposal 요청 수신");
        System.out.println("  파일명: " + file.getOriginalFilename());
        System.out.println("  파일크기: " + file.getSize() + " bytes");
        System.out.println("  테마: " + theme);
        System.out.println("  키워드: " + keywords);
        System.out.println("  제목: " + title);
        System.out.println("========================================");

        // 크레딧이 모자라면 402 (여기서 막아 아래 catch 의 500 으로 바뀌지 않게 한다)
        Charge charge = charge("analyze-proposal", "기획서 분석");
        try {
            System.out.println("[Controller] Service 호출 시작...");
            ProposalMetadata metadata = projectService.analyzeProposal(file, theme, keywords, title);
            System.out.println("[Controller] Service 호출 성공!");
            System.out.println("[Controller] 응답 데이터: " + (metadata != null ? "존재" : "null"));
            return ResponseEntity.ok(metadata);
        } catch (Exception e) {
            System.err.println("========================================");
            System.err.println("[Controller] ❌ ERROR 발생!");
            System.err.println("  에러 타입: " + e.getClass().getName());
            System.err.println("  에러 메시지: " + e.getMessage());
            System.err.println("========================================");
            e.printStackTrace();
            charge.refund();
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * 기획서 분석 → 트렌드 → 지역 트렌드를 백그라운드 작업으로 (트러블슈팅 #32).
     * 예전에는 화면이 세 요청을 차례로 보내고 끝날 때까지 로딩 화면에 묶여 있었고, 새로고침하면 결과를 잃었다.
     * 프로젝트는 여기서 만들어 작업 번호와 함께 돌려주고, 화면은 GET /api/jobs/{id} 로 진행을 본다.
     * 지역 트렌드가 실패해도 기획서·트렌드 결과는 살리고 그 몫의 크레딧만 환불한다.
     */
    @PostMapping("/analyze/jobs")
    public Job startAnalyze(
            @RequestParam("file") MultipartFile file,
            @RequestParam("theme") String theme,
            @RequestParam("keywords") String keywords,
            @RequestParam("title") String title) throws IOException {
        String mNo = CurrentMember.no();
        Charge proposalCharge = charge("analyze-proposal", "기획서 분석");
        Charge trendCharge = charge("trend", "트렌드 분석");
        Charge regionCharge = charge("trend", "지역 트렌드 분석");
        byte[] bytes;
        int pNo;
        try {
            bytes = file.getBytes(); // 요청이 끝나면 업로드 파일은 사라지므로 미리 읽는다
            pNo = projectService.createProjectAndSaveInput(theme, keywords, title, mNo).getProjectNo();
        } catch (RuntimeException | IOException e) {
            proposalCharge.refund();
            trendCharge.refund();
            regionCharge.refund();
            throw e;
        }
        String filename = file.getOriginalFilename();
        String mainKeyword = firstKeyword(keywords);

        return jobService.start(pNo, mNo, GenerationJobService.ANALYZE_PROPOSAL, progress -> {
            progress.step("proposal", "기획서 분석", jobService.expectSeconds("proposal", 30))
                    .step("trend", "트렌드 분석", jobService.expectSeconds("trend", 40))
                    .step("region_trend", "지역 트렌드 분석", jobService.expectSeconds("region_trend", 30));

            ProposalMetadata proposal = progress.run("proposal", () -> {
                try {
                    return projectService.analyzeProposal(pNo, bytes, filename, theme, keywords, title);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
            String start = proposal.getFestivalStartDate() == null ? ""
                    : new java.text.SimpleDateFormat("yyyy-MM-dd").format(proposal.getFestivalStartDate());

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("pNo", pNo);
            result.put("proposal", proposal);
            result.put("festivalStartDate", start);

            Map<String, Object> trend = progress.run("trend", () -> {
                try {
                    return projectService.analyzeTotalTrend(mainKeyword, proposal.getTitle(), start);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
            if (trend == null || trend.containsKey("error")) {
                trendCharge.refund();
                result.put("trend", null);
                result.put("trendError", "트렌드를 분석하지 못했습니다.");
            } else {
                result.put("trend", trend);
            }

            // 지역 트렌드 실패는 작업 실패가 아니다 (단계 안에서 받아 결과에 사유를 남긴다)
            RegionTrendResponseDTO region = progress.run("region_trend", () -> {
                try {
                    return projectService.analyzeRegionTrendFor(pNo, start);
                } catch (RuntimeException e) {
                    log.warn("[analyze] 지역 트렌드 실패 pNo={}: {}", pNo, e.getMessage());
                    return null;
                }
            });
            result.put("regionTrend", region);
            if (region == null) {
                regionCharge.refund();
                result.put("regionTrendError", "지역 트렌드를 분석하지 못했습니다.");
            }
            return result;
        }, () -> {
            // 기획서 분석이 실패하면 작업 전체 실패: 아직 쓰지 않은 몫까지 모두 환불 (환불은 한 번만 된다)
            proposalCharge.refund();
            trendCharge.refund();
            regionCharge.refund();
        });
    }

    /** keywords 는 화면이 보내는 JSON 배열 문자열 ["담양","산타"] */
    private static String firstKeyword(String keywords) {
        String s = keywords == null ? "" : keywords.trim();
        if (s.startsWith("[")) {
            s = s.substring(1, s.length() - 1);
        }
        String first = s.split(",")[0].trim();
        return first.replaceAll("^\"|\"$", "");
    }

    @GetMapping("/analyze/lastst")
    public ProposalMetadata getProposalMetadata() {
        return projectService.getLatestProposalMetadata();
    }

    @PostMapping("/analyze/total_trend")
    public ResponseEntity<?> analyzeTotalTrend(
            @RequestParam("keyword") String keyword,
            @RequestParam("title") String title,
            @RequestParam("festival_start_date") String festivalStartDate) throws IOException {
        log.info("📌 요청 수신: keyword={}, title={}", keyword, title);

        Charge charge = charge("trend", "트렌드 분석");
        try {
            var result = projectService.analyzeTotalTrend(keyword, title, festivalStartDate);
            // 서비스는 실패하면 예외 대신 {error: ...} 를 돌려준다
            if (result != null && result.containsKey("error")) {
                charge.refund();
            }
            return ResponseEntity.ok(result);
        } catch (RuntimeException | IOException e) {
            charge.refund();
            throw e;
        }
    }

    @PostMapping("/analyze/region_trend")
    public ResponseEntity<RegionTrendResponseDTO> analyzeRegionTrend(
            @RequestParam(value = "festival_start_date", required = false) String festivalStartDate,
            HttpServletRequest request) {
        // 1. 회원 ID 가져오기
        // (인터셉터 등에서 request attribute에 m_no를 넣어준다고 가정)
        String m_no = (String) request.getAttribute("m_no");
        if (m_no == null)
            m_no = "M000001";

        log.info("📌 [Controller] 지역 트렌드 요청: 회원={}, 날짜={}", m_no, festivalStartDate);
        Charge charge = charge("trend", "지역 트렌드 분석");
        try {
            RegionTrendResponseDTO result = projectService.analyzeRegionTrend(m_no, festivalStartDate);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            charge.refund();
            throw e;
        }
    }

}
