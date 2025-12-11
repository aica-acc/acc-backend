// src/main/java/com/assistant/acc/domain/editor/EditorTemplate.java
package com.assistant.acc.domain.editor;

import lombok.Data;
import java.util.Date;

@Data
public class EditorTemplate {

    private Integer editorTemplateId;  // editor_template_id
    private Integer pNo;               // p_no (FK to project)
    private String filePath;           // file_path

    private Date createdAt;            // created_at
    private Date updatedAt;            // updated_at
}
