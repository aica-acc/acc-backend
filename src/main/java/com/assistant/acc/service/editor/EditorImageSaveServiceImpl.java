package com.assistant.acc.service.editor;

import com.assistant.acc.dto.editor.PromotionPathDTO;
import com.assistant.acc.dto.editor.SaveImageResponse;
import com.assistant.acc.mapper.editor.PromotionPathMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class EditorImageSaveServiceImpl implements EditorImageSaveService {

    @Value("${react.base-path}")
    private String baseDir;

    private final PromotionPathMapper promotionPathMapper;

    // memberNo는 하드코딩 (필요시 ProjectMapper로 조회 가능)
    private static final String MEMBER_NO = "M000001";

    @Override
    @Transactional
    public SaveImageResponse saveEditorImage(Integer pNo, String imageBase64, String imagePath, String dbFileType) {
        try {
            log.info("💾 [EditorImageSaveService] 저장 시작: pNo={}, dbFileType={}, imagePath={}", 
                    pNo, dbFileType, imagePath != null ? "제공됨" : "없음");

            String dbFilePath;

            // ⭐ 경로 기반 저장 (우선 사용)
            if (imagePath != null && !imagePath.trim().isEmpty()) {
                dbFilePath = processImagePath(pNo, imagePath, dbFileType);
            } 
            // ⭐ base64 기반 저장 (하위 호환성)
            else if (imageBase64 != null && !imageBase64.trim().isEmpty()) {
                dbFilePath = processBase64Image(pNo, imageBase64, dbFileType);
            } 
            else {
                throw new IllegalArgumentException("imagePath 또는 imageBase64 중 하나는 필수입니다.");
            }

            // promotion_path 테이블에 저장
            PromotionPathDTO dto = new PromotionPathDTO();
            dto.setPNo(pNo);
            dto.setDbFilePath(dbFilePath);
            dto.setDbFileType(dbFileType);
            dto.setCreateAt(LocalDate.now());

            promotionPathMapper.insertPromotionPath(dto);
            log.info("💾 DB 저장 완료: {}", dto);

            // 응답 생성
            SaveImageResponse response = new SaveImageResponse();
            response.setSuccess(true);
            response.setSavedPath(dbFilePath);
            response.setMessage("이미지 저장 완료");

            return response;

        } catch (Exception e) {
            log.error("❌ [EditorImageSaveService] 저장 실패", e);
            SaveImageResponse response = new SaveImageResponse();
            response.setSuccess(false);
            response.setMessage("이미지 저장 중 오류 발생: " + e.getMessage());
            return response;
        }
    }

    /**
     * 경로 기반 이미지 처리 (이미 존재하는 파일의 경로를 DB에 저장)
     */
    private String processImagePath(Integer pNo, String imagePath, String dbFileType) {
        log.info("📁 [경로 기반 저장] 경로 처리 시작: {}", imagePath);

        // 1. 절대 경로를 상대 경로로 변환
        String relativePath = convertToRelativePath(imagePath);
        
        // 2. 파일 존재 확인 (선택적, 경고만 출력)
        String fullPath = convertToFullPath(relativePath);
        File file = new File(fullPath);
        if (!file.exists()) {
            log.warn("⚠️ [경로 기반 저장] 파일이 존재하지 않습니다: {}", fullPath);
            log.warn("⚠️ DB에는 경로만 저장됩니다. 파일은 나중에 확인이 필요합니다.");
        } else {
            log.info("✅ [경로 기반 저장] 파일 확인됨: {}", fullPath);
        }

        return relativePath;
    }

    /**
     * base64 기반 이미지 처리 (기존 로직)
     */
    private String processBase64Image(Integer pNo, String imageBase64, String dbFileType) {
        log.info("📦 [base64 기반 저장] base64 디코딩 시작");

        // 1. base64 디코딩
        String base64Data = imageBase64;
        if (base64Data.startsWith("data:image")) {
            int commaIndex = base64Data.indexOf(',');
            if (commaIndex != -1) {
                base64Data = base64Data.substring(commaIndex + 1);
            }
        }

        byte[] imageBytes = Base64.getDecoder().decode(base64Data);

        // 2. 저장 경로 생성
        String targetDir = Paths.get(
                baseDir,
                "public",
                "data",
                "promotion",
                MEMBER_NO,
                pNo.toString(),
                "editor"
        ).toString();

        File dir = new File(targetDir);
        if (!dir.exists()) {
            dir.mkdirs();
            log.info("📁 디렉토리 생성: {}", targetDir);
        }

        // 3. 파일명 생성 (타임스탬프 포함)
        String timestamp = String.valueOf(System.currentTimeMillis());
        String filename = String.format("%s_%s.png", dbFileType, timestamp);
        String filePath = Paths.get(targetDir, filename).toString();

        // 4. 파일 저장
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            fos.write(imageBytes);
            log.info("✅ 파일 저장 완료: {}", filePath);
        } catch (IOException e) {
            throw new RuntimeException("파일 저장 실패: " + filePath, e);
        }

        // 5. DB 경로 생성 (public 제외한 상대 경로)
        return String.format("/data/promotion/%s/%d/editor/%s", 
                MEMBER_NO, pNo, filename);
    }

    /**
     * 절대 경로를 상대 경로로 변환
     * 예: C:/final_project/ACC/acc-frontend/public/data/promotion/... 
     *  -> /data/promotion/...
     */
    private String convertToRelativePath(String imagePath) {
        if (imagePath == null || imagePath.trim().isEmpty()) {
            throw new IllegalArgumentException("imagePath가 비어있습니다.");
        }

        // 이미 상대 경로인 경우 (/data/로 시작)
        if (imagePath.startsWith("/data/") || imagePath.startsWith("data/")) {
            // 앞의 / 제거 후 다시 / 추가하여 정규화
            String normalized = imagePath.replace("\\", "/");
            if (!normalized.startsWith("/")) {
                normalized = "/" + normalized;
            }
            return normalized;
        }

        // 절대 경로인 경우 상대 경로로 변환
        String normalized = imagePath.replace("\\", "/");
        String publicPath = Paths.get(baseDir, "public").toString().replace("\\", "/");
        
        if (normalized.startsWith(publicPath)) {
            // public 폴더 이후의 경로 추출
            String relative = normalized.substring(publicPath.length());
            if (!relative.startsWith("/")) {
                relative = "/" + relative;
            }
            return relative;
        }

        // 변환 실패 시 원본 반환 (경고와 함께)
        log.warn("⚠️ [경로 변환] 절대 경로를 상대 경로로 변환하지 못했습니다: {}", imagePath);
        log.warn("⚠️ 원본 경로를 그대로 사용합니다.");
        return imagePath;
    }

    /**
     * 상대 경로를 절대 경로로 변환 (파일 존재 확인용)
     */
    private String convertToFullPath(String relativePath) {
        if (relativePath == null || relativePath.trim().isEmpty()) {
            return "";
        }

        String normalized = relativePath.replace("\\", "/");
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }

        // /data/로 시작하는 경우 public 폴더와 결합
        if (normalized.startsWith("/data/")) {
            return Paths.get(baseDir, "public", normalized.substring(1)).toString();
        }

        return normalized;
    }
}

