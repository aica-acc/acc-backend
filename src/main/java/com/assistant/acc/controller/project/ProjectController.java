package com.assistant.acc.controller.project;

import com.assistant.acc.service.poster.PosterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class ProjectController {

    private final PosterService posterService;

    // 생성자 주입 방식 (권장)
    @Autowired
    public ProjectController(PosterService posterService) {
        this.posterService = posterService;
    }

    // POST 요청으로 파일 + 문자열 데이터 받기
    @PostMapping("/project/analyze")
    public ResponseEntity<String> analyzeProposal(
            @RequestParam("file") MultipartFile file,
            @RequestParam("theme") String theme,
            @RequestParam("keywords") String keywords,
            @RequestParam("title") String title) {
        try {
            String result = posterService.analyze(file, theme, keywords, title);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }
}

