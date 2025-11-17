package com.assistant.acc.controller.project;

import com.assistant.acc.domain.project.ProposalMetadata;
import com.assistant.acc.service.poster.PosterService;
import com.assistant.acc.service.project.ProjectService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class ProjectController {

    private final ProjectService projectService;
    // 생성자 주입 방식 (권장)
    
    @Autowired
    public ProjectController(PosterService posterService, ProjectService projectService ) {
        this.projectService = projectService;
    }

    // POST 요청으로 파일 + 문자열 데이터 받기
    @PostMapping("/project/analyze/proposal")
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

    // @PostMapping("/project/analyze/total_trend")
    // public ResponseEntity<String> analyzeTotalTrend(
    //         @RequestParam("keywords") String keywords,
    //         @RequestParam("title") String title) {
    //     try {
    //         String metadata  = projectService.analyzeTrned(file, theme, keywords, title);
    //         return ResponseEntity.ok(metadata );
    //     } catch (Exception e) {
    //         return ResponseEntity.status(500).body(e.getMessage());
    //     }
    // }


    @GetMapping("/project/analyze/lastst")
    public ProposalMetadata getProposalMetadata() {
        return projectService.getLatestProposalMetadata();
    }
}
