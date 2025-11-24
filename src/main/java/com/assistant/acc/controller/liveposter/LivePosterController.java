package com.assistant.acc.controller.liveposter;

import com.assistant.acc.service.liveposter.LivePosterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/liveposter")
@RequiredArgsConstructor
public class LivePosterController {

    private final LivePosterService livePosterService;

    @PostMapping("/create")
    public ResponseEntity<?> createLivePoster(
            @RequestParam Integer pNo,       // Integer로 변경
            @RequestParam Integer posterNo   // Integer로 변경
    ) {
        try {
            livePosterService.createLivePoster(pNo, posterNo);
            return ResponseEntity.ok().body(Map.of("message", "라이브 포스터 생성 완료"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}