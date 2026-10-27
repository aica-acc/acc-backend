package com.assistant.acc.controller.project;

import com.assistant.acc.domain.project.ProposalMetadata;
import com.assistant.acc.dto.project.RegionTrendResponseDTO;
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
    // 생성자 주입 방식 (권장)

    @Autowired
    public ProjectController(PosterService posterService, ProjectService projectService,
            ProjectDeleteService projectDeleteService) {
        this.projectService = projectService;
        this.projectDeleteService = projectDeleteService;
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

        var result = projectService.analyzeTotalTrend(keyword, title, festivalStartDate);
        return ResponseEntity.ok(result);
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
        RegionTrendResponseDTO result = projectService.analyzeRegionTrend(m_no, festivalStartDate);

        return ResponseEntity.ok(result);
    }

}
