package com.assistant.acc.mapper.poster.regenerate;

import com.assistant.acc.domain.poster.RegenerateAssetDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RegenerateMapper {
    RegenerateAssetDetail getRegenerateAssetDetail(@Param("filePathNo") Integer filePathNo);

    int updatePromptVisual(@Param("promptNo") Integer promptNo,
                           @Param("newPrompt") String newPrompt);

    int updateFilePath(@Param("filePathNo") Integer filePathNo,
                       @Param("filePath") String filePath,
                       @Param("fileName") String fileName,
                       @Param("extension") String extension);

    int touchGeneratedAssetUpdateAt(@Param("generatedAssetNo") Integer generatedAssetNo);
}
