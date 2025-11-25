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
}
