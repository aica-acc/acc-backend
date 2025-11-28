package com.assistant.acc.mapper.editor;

import com.assistant.acc.domain.editor.EditorTemplate;
import org.apache.ibatis.annotations.Mapper;


@Mapper
public interface EditorTemplateMapper {

    void insertEditorTemplate(EditorTemplate template);

    EditorTemplate selectLatestByProjectNo(Integer pNo);
}
