// src/main/java/com/assistant/acc/dto/editor/EditorAiRenderResponse.java
package com.assistant.acc.dto.editor;

import lombok.Data;
import java.util.Map;

/**
 * AI 렌더링 결과 응답 DTO
 * - status: "success" / "error"
 * - updatedCanvas: 수정된 캔버스 JSON (스타일 반영된 결과)
 * - message: 상태나 설명
 */
@Data
public class EditorAiRenderResponse {

    private String status;                    // success / error
    private Map<String, Object> updatedCanvas; // 스타일이 적용된 Fabric JSON
    private String message;                   // 에러나 상태 메시지
}
