package com.assistant.acc.controller.project;

import com.assistant.acc.domain.project.ProposalMetadata;
import com.assistant.acc.dto.project.RegionTrendResponseDTO;
import com.assistant.acc.security.CurrentMember;
import com.assistant.acc.service.credit.CreditService;
import com.assistant.acc.service.credit.CreditService.Charge;
import com.assistant.acc.service.poster.PosterService;
import com.assistant.acc.service.project.ProjectDeleteService;
import com.assistant.acc.service.project.ProjectService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import java.util.Map;
import java.io.IOException;

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
    // 생성자 주입 방식 (권장)

    @Autowired
    public ProjectController(PosterService posterService, ProjectService projectService,
            ProjectDeleteService projectDeleteService, CreditService creditService) {
        this.projectService = projectService;
        this.projectDeleteService = projectDeleteService;
        this.creditService = creditService;
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
