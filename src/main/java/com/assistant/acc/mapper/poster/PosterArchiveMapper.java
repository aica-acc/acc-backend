package com.assistant.acc.mapper.poster;

import com.assistant.acc.dto.image.PosterElementDTO;
import com.assistant.acc.dto.poster.PosterArchiveDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PosterArchiveMapper {

    // 1. 저장 (기존)
    void save(PosterArchiveDTO posterArchiveDTO);

    // 2. ✅ [NEW] 단일 조회 (재생성 시 정보 확인용)
    PosterArchiveDTO findById(@Param("filePathNo") Integer filePathNo);

    // 3. ✅ [NEW] 프로젝트별 포스터 목록 조회 (프론트엔드 리스트용)
    List<PosterElementDTO> findPromptsByProjectNo(@Param("projectNo") Integer projectNo);

    // 4. ✅ [NEW] 이미지 업데이트 (재생성 결과 반영용)
    void updatePosterImage(@Param("filePathNo") Integer filePathNo,
                           @Param("fileUrl") String fileUrl,
                           @Param("visualPrompt") String visualPrompt);

    List<PosterArchiveDTO> findPosters(String year, String theme);
}