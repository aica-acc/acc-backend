// src/main/java/com/assistant/acc/service/editor/EditorAiRenderService.java
package com.assistant.acc.service.editor;

import com.assistant.acc.dto.editor.EditorAiRenderRequest;
import com.assistant.acc.dto.editor.EditorAiRenderResponse;

public interface EditorAiRenderService {

    EditorAiRenderResponse renderWithAi(EditorAiRenderRequest request);
}
