package com.assistant.acc.controller.poster;

import com.assistant.acc.dto.image.ImageRegenerateResponseDTO;
import com.assistant.acc.dto.image.PosterElementDTO;
import com.assistant.acc.service.poster.PosterService;
import com.assistant.acc.dto.poster.PosterPromptRequest;
import com.assistant.acc.dto.poster.PosterPromptResponse;
import com.assistant.acc.dto.poster.PosterCreateRequest;
import com.assistant.acc.dto.poster.PosterCreateResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api") // 기본 경로
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5175"})
public class PosterController {

    private final PosterService posterService;

    public PosterController(PosterService posterService) {
        this.posterService = posterService;
    }

    // [기존] 1단계 분석
    @PostMapping("/analyze/poster")
    public ResponseEntity<String> analyzePoster(
            @RequestParam("file") MultipartFile file,
            @RequestParam("theme") String theme,
            @RequestParam("keywords") String keywords,
            @RequestParam("title") String title) {
        try {
            return ResponseEntity.ok(posterService.analyze(file, theme, keywords, title));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // [기존] 2단계 프롬프트 생성
    @PostMapping("/generate-prompt")
    public ResponseEntity<PosterPromptResponse> generatePrompt(@RequestBody PosterPromptRequest requestDto) {
        // Service가 이제 DTO를 반환
        return ResponseEntity.ok(posterService.generatePrompt(requestDto));
    }

    // [기존] 3단계 이미지 생성
    @PostMapping("/create-image")
    public ResponseEntity<PosterCreateResponse> createImage(@RequestBody PosterCreateRequest requestDto) {
        return ResponseEntity.ok(posterService.createImage(requestDto));
    }

    // 포스터 목록 조회 (Poster.basePosterInfo 대응)
    // URL: /api/posters/{pNo}/elements
    @GetMapping("/posters/{pNo}/elements")
    public ResponseEntity<List<PosterElementDTO>> getPosterElements(@PathVariable Integer pNo) {
        List<PosterElementDTO> list = posterService.getPosterPrompts(pNo);
        return ResponseEntity.ok(list);
    }

    // 포스터 재생성 (Poster.updatePosterInfo 대응)
    // URL: /api/posters/{filePathNo}/regenerate
    @PostMapping("/posters/{filePathNo}/regenerate")
    public ResponseEntity<?> regeneratePoster(
            @PathVariable Integer filePathNo,
            @RequestBody Map<String, String> body) {
        try {
            String visualPrompt = body.get("visual_prompt");
            ImageRegenerateResponseDTO result = posterService.regeneratePoster(filePathNo, visualPrompt);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "재생성 실패: " + e.getMessage()));
        }
    }

}