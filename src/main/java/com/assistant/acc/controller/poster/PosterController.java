package com.assistant.acc.controller.poster;

import com.assistant.acc.domain.poster.RegenerateResponseDTO;
import com.assistant.acc.domain.prompt.Prompt;
import com.assistant.acc.dto.create.poster.CreateImageResultResponse;
import com.assistant.acc.dto.image.PosterElementDTO;
import com.assistant.acc.service.poster.generate.PromotionAPIService;
import com.assistant.acc.service.poster.PosterService;
import com.assistant.acc.service.poster.regenerate.PosterRegenerateService;
import jakarta.servlet.http.HttpServletRequest;
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
    private final PromotionAPIService promotionAPIService;
    private final PosterRegenerateService posterRegenerateService;

    public PosterController(PosterService posterService, PromotionAPIService promotionAPIService, PosterRegenerateService posterRegenerateService) {
        this.posterService = posterService;
        this.promotionAPIService = promotionAPIService;
        this.posterRegenerateService = posterRegenerateService;
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
    public ResponseEntity<List<Prompt>> generatePrompt(
            @RequestBody Map<String, Object> trendData,
            @RequestParam String promotionType,
            HttpServletRequest request
            ) {
        // Service가 이제 DTO를 반환
        String m_no = (String) request.getAttribute("m_no");
        if(m_no == null) m_no = "M000001";
        List<Prompt> result = promotionAPIService.generatePrompts(m_no, trendData, promotionType);
        return ResponseEntity.ok(result);
    }

    // [기존] 3단계 이미지 생성
    @PostMapping("/create-image")
    public ResponseEntity<CreateImageResultResponse> createImage(
            @RequestBody Map<String, Object> trendData,
            @RequestParam String promotionType,
            HttpServletRequest request

    ) {
        String m_no = (String) request.getAttribute("m_no");
        if(m_no == null) m_no = "M000001";
        CreateImageResultResponse result = promotionAPIService.createPosterImages(m_no, trendData, promotionType);
        return ResponseEntity.ok(result);
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
    public ResponseEntity<RegenerateResponseDTO> regeneratePoster(
            @PathVariable Integer filePathNo,
            @RequestParam String promotionType,
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {

        String m_no = (String) request.getAttribute("m_no");
        if(m_no == null) m_no = "M000001";
        String visualPrompt = (String) body.get("visual_prompt");

        RegenerateResponseDTO result = posterRegenerateService.regenerated(m_no, filePathNo, visualPrompt, promotionType);

        return ResponseEntity.ok(result);
    }

}