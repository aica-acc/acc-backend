package com.assistant.acc.service.image;

import com.assistant.acc.dto.image.ImageDetailDTO;
import com.assistant.acc.dto.image.ImageRegenerateResponseDTO;
import com.assistant.acc.dto.image.PosterElementDTO;
import com.assistant.acc.dto.poster.PosterArchiveDTO;
import com.assistant.acc.mapper.image.ImageMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class ImageServiceImpl implements ImageService {

    private final ImageMapper imageMapper;

    public ImageServiceImpl(ImageMapper imageMapper) {
        this.imageMapper = imageMapper;
    }

    @Override
    public ImageDetailDTO getImageDetail(Integer filePathNo) {
        // ImageMapper 또는 PosterArchiveMapper를 통해 조회
        return imageMapper.findImageDetailById(filePathNo);
    }

    @Override
    public List<PosterElementDTO> getProjectImages(Integer projectNo) {
        // 프로젝트의 포스터 목록 조회
        return imageMapper.findPromptsByProjectNo(projectNo);
    }

    @Override
    public ImageRegenerateResponseDTO regenerateImage(Integer filePathNo, String visualPrompt) throws IOException {
        // 기존 이미지 정보 조회
        PosterArchiveDTO existing = imageMapper.findById(filePathNo);
        if (existing == null) {
            throw new IOException("해당 이미지를 찾을 수 없습니다. ID: " + filePathNo);
        }
        String updatedUrl = existing.getImageUrl(); // 실제로는 새 이미지 URL로 업데이트 필요

        // DB 업데이트
        imageMapper.updateImage(filePathNo, updatedUrl, visualPrompt);

        // 반환 객체 생성
        ImageRegenerateResponseDTO response = new ImageRegenerateResponseDTO();
        response.setFilePathNo(filePathNo);
        response.setFileUrl(updatedUrl);
        response.setVisualPrompt(visualPrompt);
        response.setRegenerated(true);
        response.setMessage("success");

        return response;
        }
    }