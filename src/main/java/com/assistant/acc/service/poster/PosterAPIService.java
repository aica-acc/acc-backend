package com.assistant.acc.service.poster;

import com.assistant.acc.domain.prompt.Prompt;
import com.assistant.acc.dto.create.CreateImageResultResponse;

import java.util.List;
import java.util.Map;

public interface PosterAPIService {
    List<Prompt> generatePrompts(String memberNo, Map<String, Object> trendData);

    CreateImageResultResponse createPosterImages(String memberNo, Map<String, Object> trendData);
}
