package com.assistant.acc.mapper.file;

import com.assistant.acc.domain.file.AssetDetail;
import com.assistant.acc.domain.file.AssetElement;
import com.assistant.acc.dto.file.AssetDetailRequestDto;
import com.assistant.acc.dto.file.AssetQueryRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AssetsMapper {
    // p_no에 종속된 모든 홍보물 조회
    List<AssetElement> selectAssetElementList(AssetQueryRequest request);
    // 파일 detail 데이터
    AssetDetail getAssetDetail(AssetDetailRequestDto request);
}
