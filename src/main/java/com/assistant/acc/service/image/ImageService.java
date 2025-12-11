package com.assistant.acc.service.image;

import com.assistant.acc.dto.image.ImageDetailDTO;
import com.assistant.acc.dto.image.ImageRegenerateResponseDTO;
import com.assistant.acc.dto.image.PosterElementDTO;

import java.io.IOException;
import java.util.List;

public interface ImageService {
    // 1. 단일 이미지 조회
    ImageDetailDTO getImageDetail(Integer filePathNo);

    // 2. 프로젝트의 이미지(포스터) 목록 조회
    List<PosterElementDTO> getProjectImages(Integer projectNo);

    // 3. 이미지 재생성
    ImageRegenerateResponseDTO regenerateImage(Integer filePathNo, String visualPrompt) throws IOException;
}
