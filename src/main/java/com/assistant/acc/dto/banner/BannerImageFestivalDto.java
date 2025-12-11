// src/main/java/com/assistant/acc/dto/banner/BannerImageFestivalDto.java
package com.assistant.acc.dto.banner;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 배너/현수막 트렌드 분석 결과에서 사용하는 "축제 1개" 정보 DTO.
 *
 * Python 응답 예:
 * {
 *   "festival_id": "...",
 *   "festival_name": "...",
 *   "banner_image_url": "...",
 *   "banner_image_description": "...",
 *   "start_date": "2025-01-01",
 *   "end_date": "2025-01-07",
 *   "region": "...",
 *   "score": 3   // related_festivals 에만 존재, latest_festivals에는 없을 수 있음
 * }
 */
public record BannerImageFestivalDto(

        @JsonProperty("festival_id")
        String festivalId,

        @JsonProperty("festival_name")
        String festivalName,

        @JsonProperty("banner_image_url")
        String bannerImageUrl,

        @JsonProperty("banner_image_description")
        String bannerImageDescription,

        @JsonProperty("start_date")
        String startDate,

        @JsonProperty("end_date")
        String endDate,

        @JsonProperty("region")
        String region,

        // related_festivals 에만 있는 필드 (latest에는 없을 수 있으므로 Integer)
        @JsonProperty("score")
        Integer score
) {}
