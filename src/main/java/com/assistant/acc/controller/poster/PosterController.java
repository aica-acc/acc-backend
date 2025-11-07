package com.assistant.acc.controller.poster;

import java.io.IOException; // ⭐️ 서비스 import

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping; // ⭐️ IO 예외 import
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.assistant.acc.service.poster.PosterService;

@RestController
@RequestMapping("/api/poster")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5175"})
public class PosterController {

    private final PosterService posterService;

    public PosterController(PosterService posterService) {
        this.posterService = posterService;
    }

    /**
     * [API 1] 1단계 UI: "분석" 버튼용 (React -> Java)
     * React의 /analyze 요청을 받아 Python의 /analyze로 전달.
     */
    @PostMapping("/analyze")
    public ResponseEntity<String> analyzePoster(
            @RequestParam("file") MultipartFile file,
            @RequestParam("theme") String theme,
            @RequestParam("keywords") String keywords,
            @RequestParam("title") String title) {

        try {
            // ervice에게 모든 작업을 위임
            String pythonResponse = posterService.analyze(file, theme, keywords, title);

            // Python이 반환한 JSON 문자열을 React에게 그대로 전달
            return ResponseEntity.ok(pythonResponse);

        } catch (IOException e) {
            // (PosterService에서 DB/Python 오류가 발생하면 이리로 옴)
            return ResponseEntity.status(500).body("{\"status\": \"error\", \"message\": \"백엔드 처리 중 오류: " + e.getMessage() + "\"}");
        } catch (Exception e) {
            // (그 외 예상치 못한 서버 오류)
            return ResponseEntity.status(502).body("{\"status\": \"error\", \"message\": \"기타 서버 통신 실패: " + e.getMessage() + "\"}");
        }
    }
}