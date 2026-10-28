package com.assistant.acc.controller.image;

import com.assistant.acc.dto.image.ImageDetailDTO;
import com.assistant.acc.service.image.ImageService;
import com.assistant.acc.dto.image.PosterElementDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/images")
public class ImageController {

    private final ImageService imageService;

    public ImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    // 이미지 조회 (Image.getImage 대응)
    // URL: /api/images/{filePathNo}
    @GetMapping("/{filePathNo}")
    public ResponseEntity<ImageDetailDTO> getImage(@PathVariable Integer filePathNo) {
        ImageDetailDTO image = imageService.getImageDetail(filePathNo);
        return ResponseEntity.ok(image);
    }
    // 2. 프로젝트 번호로 이미지 목록 조회 (포스터, 마스코트, 배너 등)
    @GetMapping("/project/{projectNo}")
    public ResponseEntity<List<PosterElementDTO>> getProjectImages(@PathVariable Integer projectNo) {
        System.out.println("🖼️ [ImageController] 프로젝트 이미지 전체 조회 요청: " + projectNo);
        List<PosterElementDTO> images = imageService.getProjectImages(projectNo);
        return ResponseEntity.ok(images);
    }
}