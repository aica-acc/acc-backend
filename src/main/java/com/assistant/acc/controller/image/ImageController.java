package com.assistant.acc.controller.image;

import com.assistant.acc.dto.image.ImageDetailDTO;
import com.assistant.acc.service.image.ImageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/images")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5175"})
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
}