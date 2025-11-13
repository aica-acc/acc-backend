package com.assistant.acc.controller.poster;

import java.io.IOException; // ⭐️ 서비스 import

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.assistant.acc.service.poster.PosterService;

@RestController
@RequestMapping("/api")
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
    @PostMapping("/analyze/poster")
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
    /**
     * 2단계 API: AI 프롬프트 생성 요청 (JSON Proxy)
     * Postman/프론트엔드에서 JSON을 받아 PosterService로 전달합니다.
     */
    @PostMapping("/generate-prompt")
    public ResponseEntity<String> generatePrompt(@RequestBody String jsonBody) {
        try {
            // 서비스의 2단계 메소드 호출
            String aiResponse = posterService.generatePrompt(jsonBody);

            // Python이 보낸 JSON을 그대로 반환 (Content-Type을 JSON으로 설정)
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            return new ResponseEntity<>(aiResponse, headers, HttpStatus.OK);
        } catch (Exception e) {
            // 오류처리
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"status\": \"error\", \"message\": \"" + e.getMessage() + "\"}");
        }
    }
    /**
     * 3단계 API: 최종 홍보물 생성 요청 (JSON Proxy)
     * Postman/프론트엔드에서 JSON을 받아 PosterService로 전달합니다.
     */
    @PostMapping("/create-image")
    public ResponseEntity<String> createImage(@RequestBody String jsonBody) {
        try{
            // 서비스의 3단계 메소드 호출
            String aiResponse = posterService.createImage(jsonBody);

            // Python이 보낸 JSON을 그대로 반환
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            return new ResponseEntity<>(aiResponse, headers, HttpStatus.OK);

        } catch (Exception e) {
            // (오류 처리는 analyze 메소드와 동일하게 구성)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"status\": \"error\", \"message\": \"" + e.getMessage() + "\"}");
        }
    }
}