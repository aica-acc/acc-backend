package com.assistant.acc.mapper.project.promotion;

import com.assistant.acc.domain.project.promotion.GeneratedAsset;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface GeneratedAssetMapper {
    int AssetInsert(GeneratedAsset asset);

    GeneratedAsset AssetSelect(@Param("generatedAssetNo") Integer generatedAssetNo);

    List<GeneratedAsset> AssetSelects(@Param("promotionNo") Integer promotionNo);

    int AssetUpdate(GeneratedAsset asset);

    int AssetDelete(@Param("generatedAssetNo") Integer generatedAssetNo);
}
