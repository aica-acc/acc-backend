// src/main/java/com/assistant/acc/dto/banner/BannerImageTrendResponse.java
package com.assistant.acc.dto.banner;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * 배너 이미지 트렌드 분석 응답 DTO.
 *
 * Python /banner/analyze-image 응답 구조와 1:1 매핑.
 *
 * {
 *   "related_festivals": [ BannerImageFestivalDto... 최대 5개 ],
 *   "latest_festivals":  [ BannerImageFestivalDto... 최대 5개 ]
 * }
 */
public record BannerImageTrendResponse(

        @JsonProperty("related_festivals")
        List<BannerImageFestivalDto> relatedFestivals,

        @JsonProperty("latest_festivals")
        List<BannerImageFestivalDto> latestFestivals
) {}
