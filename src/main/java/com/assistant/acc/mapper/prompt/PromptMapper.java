package com.assistant.acc.mapper.prompt;

import com.assistant.acc.domain.prompt.Prompt;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PromptMapper {
    // Create
    int insertPrompt(Prompt prompt);
    // Read
    Prompt selectPrompt(@Param("promptNo") Integer promptNo);
    List<Prompt> selectPrompts(@Param("userInputNo") Integer userInputNo);
    List<Prompt> selectPromptsByType(
            @Param("userInputNo") Integer userInputNo,
            @Param("promotionType") String promotionType);

    // Update
    int updatePrompt(Prompt prompt);
    // Delete
    int delete(@Param("promptNo") Integer promptNo);
}
