// src/main/java/com/assistant/acc/dto/editor/EditorAiRenderRequest.java
package com.assistant.acc.dto.editor;

import lombok.Data;
import java.util.Map;

/**
 * 에디터 캔버스 기반 AI 렌더링 요청 DTO
 * - backgroundImage: Fabric 캔버스의 배경 이미지 (toDataURL() 결과)
 * - canvasJson: 캔버스 오브젝트 전체 JSON (layout + text + style)
 * - layoutType: 레이아웃 타입 ("banner_4_1", "poster_vertical", "card_news" 등)
 * - pNo: 프로젝트 번호 (선택)
 * - model: 추후 AI 모델 지정용 (선택)
 */
@Data
public class EditorAiRenderRequest {

    private Integer pNo;                    // 프로젝트 번호
    private String layoutType;              // 예: "banner_4_1", "poster_vertical"
    private String backgroundImage;         // data:image/png;base64,.... 형태
    private Map<String, Object> canvasJson; // Fabric 캔버스 JSON 전체
    private String model;                   // optional (ex: "gemini-3-pro-image-preview")
}
