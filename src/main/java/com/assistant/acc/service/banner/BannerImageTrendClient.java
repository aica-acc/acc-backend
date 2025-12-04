// src/main/java/com/assistant/acc/service/banner/BannerImageTrendClient.java
package com.assistant.acc.service.banner;

import com.assistant.acc.dto.banner.BannerTrendAnalyzeRequest;
import com.assistant.acc.dto.banner.BannerImageTrendResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * acc-ai 서버(파이썬 FastAPI)의
 *   POST /banner/analyze-image
 * 엔드포인트를 호출하는 클라이언트.
 */
@Component
@RequiredArgsConstructor
public class BannerImageTrendClient {

    private final RestTemplate restTemplate;

    @Value("${accai.base-url:http://localhost:5000}")
    private String accAiBaseUrl;

    /**
     * 축제명/테마/키워드를 넘겨서,
     * acc-ai 에게 "관련 축제 5개 + 최신 축제 5개"를 요청한다.
     */
    public BannerImageTrendResponse analyzeBannerImageTrend(BannerTrendAnalyzeRequest request) {
        String url = accAiBaseUrl + "/banner/analyze-image";

        // Python 쪽 BannerAnalyzeRequest 형식에 맞춰서 Body 생성
        Map<String, Object> body = new HashMap<>();
        body.put("p_name", request.festivalName());
        body.put("user_theme", request.festivalTheme());
        body.put("keywords", request.keywords());

        try {
            return restTemplate.postForObject(
                    url,
                    body,
                    BannerImageTrendResponse.class
            );
        } catch (HttpStatusCodeException e) {
            String msg = "acc-ai /banner/analyze-image 호출 실패: "
                    + e.getStatusCode() + " " + e.getResponseBodyAsString();
            throw new RuntimeException(msg, e);
        } catch (Exception e) {
            throw new RuntimeException("acc-ai /banner/analyze-image 호출 중 예기치 못한 오류", e);
        }
    }
}
