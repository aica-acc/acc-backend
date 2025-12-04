// src/main/java/com/assistant/acc/service/banner/BannerImageTrendService.java
package com.assistant.acc.service.banner;

import com.assistant.acc.dto.banner.BannerTrendAnalyzeRequest;
import com.assistant.acc.dto.banner.BannerImageTrendResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 배너 이미지 트렌드 분석 도메인 서비스.
 * - 나중에 검증/로그/다른 도메인 결합이 필요하면 여기에 로직을 추가하면 된다.
 */
@Service
@RequiredArgsConstructor
public class BannerImageTrendService {

    private final BannerImageTrendClient bannerImageTrendClient;

    public BannerImageTrendResponse analyze(BannerTrendAnalyzeRequest request) {
        // TODO: 필요하다면 여기서 null/빈 문자열 검증 추가
        return bannerImageTrendClient.analyzeBannerImageTrend(request);
    }
}
