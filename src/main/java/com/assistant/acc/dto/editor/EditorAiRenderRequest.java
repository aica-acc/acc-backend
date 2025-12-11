// src/main/java/com/assistant/acc/dto/editor/EditorAiRenderRequest.java
package com.assistant.acc.dto.editor;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.Map;

/**
 * AI 색상 추천(스타일링) 요청 DTO
 * - backgroundImage: 배경 이미지 URL 또는 경로
 * - canvasJson: 현재 캔버스 데이터 (Fabric.js JSON 객체)
 * - layoutType: 레이아웃 타입 (카테고리명, 예: "가로등 현수막", "버스정류장 광고" 등)
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EditorAiRenderRequest {

    private String layoutType;              // 레이아웃 타입 (카테고리명)
    private String backgroundImage;         // 배경 이미지 URL 또는 경로
    private Map<String, Object> canvasJson; // Fabric 캔버스 JSON 전체
}
