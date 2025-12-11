package com.assistant.acc.mapper.image;

import com.assistant.acc.dto.image.ImageDetailDTO;
import com.assistant.acc.dto.image.PosterElementDTO;
import com.assistant.acc.dto.poster.PosterArchiveDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ImageMapper {
    // 1. 단일 이미지 상세 조회
    ImageDetailDTO findImageDetailById(@Param("filePathNo")Integer filePathNo);

    // 2. 프로젝트의 포스터 프롬프트 목록 조회 (Poster.basePosterInfo)
    List<PosterElementDTO> findPromptsByProjectNo(@Param("projectNo") Integer projectNo);

    // 3. 이미지 정보 업데이트 (재생성 후)
    void updateImage(@Param("filePathNo") Integer filePathNo,
                     @Param("fileUrl") String fileUrl,
                     @Param("visualPrompt") String visualPrompt);
    // (기존 정보 조회용 - 스타일 이름 등 확인)
    PosterArchiveDTO findById(@Param("filePathNo") Integer filePathNo);
}
