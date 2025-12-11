package com.assistant.acc.service.image;

import com.assistant.acc.dto.image.ImageDetailDTO;
import com.assistant.acc.dto.image.ImageRegenerateResponseDTO;
import com.assistant.acc.dto.image.PosterElementDTO;
import com.assistant.acc.dto.poster.PosterArchiveDTO;
import com.assistant.acc.mapper.image.ImageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.assistant.acc.dto.editor.PromotionPathDTO;
import com.assistant.acc.mapper.editor.PromotionPathMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ImageServiceImpl implements ImageService {

    private final ImageMapper imageMapper;
    private final PromotionPathMapper promotionPathMapper;

//    public ImageServiceImpl(ImageMapper imageMapper) {
//        this.imageMapper = imageMapper;
//    }

    @Override
    public ImageDetailDTO getImageDetail(Integer filePathNo) {
        // ImageMapper 또는 PosterArchiveMapper를 통해 조회
        return imageMapper.findImageDetailById(filePathNo);
    }

    @Override
    public List<PosterElementDTO> getProjectImages(Integer projectNo) {
        // 1. [기존 유지] AI가 생성한 이미지들 가져오기 (poster_archive 등)
        List<PosterElementDTO> originalImages = imageMapper.findPromptsByProjectNo(projectNo);

        // 결과 리스트 초기화 (null 방지)
        List<PosterElementDTO> result = (originalImages != null) ? new ArrayList<>(originalImages) : new ArrayList<>();

        // 2. [추가] 최종 저장된 파일들 가져오기 (promotion_path 테이블)
        // (현수막, 배너 등 에디터를 거쳐 저장된 파일들)
        List<PromotionPathDTO> finalPaths = promotionPathMapper.findAllByPNo(projectNo);

        if (finalPaths != null) {
            for (PromotionPathDTO path : finalPaths) {
                PosterElementDTO dto = new PosterElementDTO();

                // 경로 설정
                dto.setFileUrl(path.getDbFilePath());

                // 타입 설정 (road_banner, poster 등)
                dto.setAssetType(path.getDbFileType());

                // promotion_path에 있는 건 최종 결과물이므로 메인/선택됨으로 표시
                dto.setIsMain(1);

                // 리스트에 추가
                result.add(dto);
            }
        }

        return result;
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