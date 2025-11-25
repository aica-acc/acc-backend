package com.assistant.acc.mapper.poster;

import com.assistant.acc.domain.create.poster.PosterElement;
import com.assistant.acc.dto.file.AssetQueryRequest;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface PosterMapper {
    // p_no 기준 해당
    List<PosterElement> selectPosterElements(AssetQueryRequest req);
}
