package com.assistant.acc.dto.project;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

import lombok.Data;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class RegionTrendResponseDTO {
    private String keyword;
    private String host;
    private String title;
    private String festivalStartDate;

    private List<Map <String, Object>> region_trend;
    //// 구조: [{"keyword": "죽녹원", "description": "...", "score": 9}, ...]
    private List<Map<String, Object>> word_cloud;
    private List<Map<String, Object>> family;
    private List<Map<String, Object>> couple;
    private List<Map<String, Object>> healing;

    /**
     * 위에 정의하지 않은 AI 응답 필드(growth_stats, google_trend, youtube_trend, region_name,
     * region_trend_simulated, notices …)도 버리지 않고 그대로 프론트에 전달한다.
     * (예전에는 이 필드들이 빠져 화면의 성장률 카드가 한 번도 표시되지 않았다)
     */
    private Map<String, Object> others = new LinkedHashMap<>();

    @JsonAnySetter
    public void putOther(String key, Object value) {
        others.put(key, value);
    }

    @JsonAnyGetter
    public Map<String, Object> getOthers() {
        return others;
    }
}
