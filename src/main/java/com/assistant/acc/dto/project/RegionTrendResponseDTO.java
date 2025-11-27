package com.assistant.acc.dto.project;

import lombok.Data;
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
}
