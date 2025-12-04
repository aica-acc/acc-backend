// src/main/java/com/assistant/acc/controller/banner/BannerImageTrendController.java
package com.assistant.acc.controller.banner;

import com.assistant.acc.dto.banner.BannerTrendAnalyzeRequest;
import com.assistant.acc.dto.banner.BannerImageTrendResponse;
import com.assistant.acc.service.banner.BannerImageTrendService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 배너 이미지 기반 트렌드 분석 컨트롤러.
 *
 * - 입력: 축제명, 축제 테마, 키워드
 * - 출력: 관련 축제 5개 + 최신 축제 5개
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/analyze")
public class BannerImageTrendController {

    private final BannerImageTrendService bannerImageTrendService;

    /**
     * 예시 요청:
     * POST /api/analyze/banner/image
     * {
     *   "festivalName": "제 7회 담양산타축제",
     *   "festivalTheme": "지구촌 최대의 겨울 축제",
     *   "keywords": ["산타", "연말", "축제"]
     * }
     *
     * 예시 응답:
     * {
     *   "related_festivals": [ { ... } ],
     *   "latest_festivals":  [ { ... } ]
     * }
     */
    @PostMapping("/banner")
    public BannerImageTrendResponse analyzeBannerImage(
            @RequestBody BannerTrendAnalyzeRequest request
    ) {
        return bannerImageTrendService.analyze(request);
    }
}
