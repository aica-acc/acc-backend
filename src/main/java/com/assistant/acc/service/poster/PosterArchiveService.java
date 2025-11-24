package com.assistant.acc.service.poster;

import com.assistant.acc.dto.poster.PosterArchiveDTO;

import java.util.List;

public interface PosterArchiveService {

    /**
     * '필터링'된 '포스터' 아카이브 목록을 반환합니다.
     * @param year '연도' (예: "2024")
     * @param theme '테마' (예: "축제")
     * @return '필터링'된 DTO 목록
     */
    List<PosterArchiveDTO> getPosters(String year, String theme);


}
