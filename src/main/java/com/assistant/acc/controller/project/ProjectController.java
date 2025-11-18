package com.assistant.acc.controller.project;

import com.assistant.acc.domain.project.ProposalMetadata;
import com.assistant.acc.service.poster.PosterService;
import com.assistant.acc.service.project.ProjectService;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/project")
public class ProjectController {

    private final ProjectService projectService;
    // 생성자 주입 방식 (권장)
    
    @Autowired
    public ProjectController(PosterService posterService, ProjectService projectService ) {
        this.projectService = projectService;
    }

    // POST 요청으로 파일 + 문자열 데이터 받기
    @PostMapping("/analyze/proposal")
    public ResponseEntity<ProposalMetadata> analyzeProposal(
            @RequestParam("file") MultipartFile file,
            @RequestParam("theme") String theme,
            @RequestParam("keywords") String keywords,
            @RequestParam("title") String title) {
        try {
            ProposalMetadata metadata  = projectService.analyzeProposal(file, theme, keywords, title);
            return ResponseEntity.ok(metadata);
        } catch (Exception e) {
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
        @RequestParam("festivalStartDate") String festivalStartDate
        ) throws IOException {
            log.info("📌 요청 수신: keyword={}, title={}", keyword, title);

            var result = projectService.analyzeTotalTrend(keyword, title, festivalStartDate);
            return ResponseEntity.ok(result);
        }


  
}

