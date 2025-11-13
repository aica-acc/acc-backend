// src/main/java/com/assistant/acc/dto/banner/BannerTrendAnalyzeRequest.java
package com.assistant.acc.dto.banner;

import java.util.List;

public record BannerTrendAnalyzeRequest(
        String pName,
        String userTheme,
        List<String> keywords
) {}
