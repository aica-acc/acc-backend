// src/main/java/com/assistant/acc/dto/editor/EditorAiRenderResponse.java
package com.assistant.acc.dto.editor;

import lombok.Data;
import java.util.Map;

/**
 * AI 색상 추천(스타일링) 결과 응답 DTO
 * - status: "success" / "error"
 * - updatedCanvas: 수정된 캔버스 JSON (색상/스타일이 추천되어 변경된 canvasData)
 * - message: 에러 메시지 (에러 발생 시)
 */
@Data
public class EditorAiRenderResponse {

    private String status;                    // success / error
    private Map<String, Object> updatedCanvas; // AI가 추천한 스타일이 적용된 Fabric 캔버스 JSON
    private String message;                   // 에러나 상태 메시지
}
