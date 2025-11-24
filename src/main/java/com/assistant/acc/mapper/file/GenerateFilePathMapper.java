package com.assistant.acc.mapper.file;

import com.assistant.acc.domain.file.GenerateFilePath;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface GenerateFilePathMapper {

    int FilePathInsert(GenerateFilePath file);

    GenerateFilePath FilePathSelect(@Param("generateFilePathNo") Integer id);

    List<GenerateFilePath> FilePathSelects(@Param("generatedAssetNo") Integer generatedAssetNo);

    int FilePathUpdate(GenerateFilePath file);

    int FilePathDelete(@Param("generateFilePathNo") Integer id);
}