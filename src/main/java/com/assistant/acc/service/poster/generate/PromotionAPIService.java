package com.assistant.acc.service.poster.generate;

import com.assistant.acc.domain.prompt.Prompt;
import com.assistant.acc.dto.create.poster.CreateImageResultResponse;

import java.util.List;
import java.util.Map;

public interface PromotionAPIService {

    List<Prompt> generatePrompts(String memberNo, Map<String, Object> trendData, String promotionType);

    CreateImageResultResponse createPosterImages(String memberNo, Map<String, Object> trendData, String promotionType);

    /** 프로젝트를 지정해 생성 (pNo 가 null 이면 회원의 최신 프로젝트). 백그라운드 작업에서 쓴다 */
    List<Prompt> generatePrompts(String memberNo, Integer pNo, Map<String, Object> trendData, String promotionType);

    CreateImageResultResponse createPosterImages(String memberNo, Integer pNo, Map<String, Object> trendData, String promotionType);

    /** 이 프로젝트에 이미 만든 이미지(DB 기록 + 파일)가 있는지. 있으면 생성 작업이 AI 를 부르지 않는다 */
    boolean hasImages(String memberNo, Integer pNo, String promotionType);
}
