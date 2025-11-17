package com.assistant.acc.mapper.poster;

import com.assistant.acc.dto.poster.PosterArchiveDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PosterArchiveMapper {

    /**
     * '필터링'된 '포스터' 목록을 'DB'에서 조회합니다.
     */
    List<PosterArchiveDTO> findPosters(
            @Param("year") String year,
            @Param("theme") String theme
    );
}
