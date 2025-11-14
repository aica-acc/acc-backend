// src/main/java/com/assistant/acc/dto/banner/BannerTrendAnalyzeRequest.java
package com.assistant.acc.dto.banner;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.List;

/**
 * 트렌드 분석 요청 DTO
 * - 프론트에서 보내는 JSON 중,
 *   축제명 / 축제테마 / 키워드만 사용.
 * - 그 외 필드는 모두 무시(ignoreUnknown=true).
 */
@JsonIgnoreProperties(ignoreUnknown = true) // DTO에 정의되지 않은 필드는 자동 무시
public record BannerTrendAnalyzeRequest(

        /**
         * 축제명
         * - 프론트에서 "festivalName" 또는 "pName" 로 보내도 받을 수 있게 JsonAlias 사용
         */
        @JsonAlias({"festivalName", "pName", "p_name"})
        String festivalName,

        /**
         * 축제 테마(기획 의도)
         * - 프론트에서 "festivalTheme" 또는 "userTheme" 로 보내도 받을 수 있게 JsonAlias 사용
         */
        @JsonAlias({"festivalTheme", "userTheme", "user_theme"})
        String festivalTheme,

        /**
         * 키워드 목록
         * - 프론트에서 그냥 "keywords": ["A","B","C"] 형태로 보내면 됨
         */
        List<String> keywords

) {}
