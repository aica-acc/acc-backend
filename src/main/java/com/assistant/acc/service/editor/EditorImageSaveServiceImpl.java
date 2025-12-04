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
    public SaveImageResponse saveEditorImage(Integer pNo, String imageBase64, String dbFileType) {
        try {
            log.info("💾 [EditorImageSaveService] 저장 시작: pNo={}, dbFileType={}", pNo, dbFileType);

            // 1. base64 디코딩
            String base64Data = imageBase64;
            if (base64Data.startsWith("data:image")) {
                // "data:image/png;base64," 부분 제거
                int commaIndex = base64Data.indexOf(',');
                if (commaIndex != -1) {
                    base64Data = base64Data.substring(commaIndex + 1);
                }
            }

            byte[] imageBytes = Base64.getDecoder().decode(base64Data);

            // 2. 저장 경로 생성
            // PUBLIC_FOLDER_PATH/data/promotion/m000001/{pNo}/editor/
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
            }

            // 5. DB 경로 생성 (public 제외한 상대 경로)
            String dbFilePath = String.format("/data/promotion/%s/%d/editor/%s", 
                    MEMBER_NO, pNo, filename);

            // 6. promotion_path 테이블에 저장
            PromotionPathDTO dto = new PromotionPathDTO();
            dto.setPNo(pNo);
            dto.setDbFilePath(dbFilePath);
            dto.setDbFileType(dbFileType);
            dto.setCreateAt(LocalDate.now());

            promotionPathMapper.insertPromotionPath(dto);
            log.info("💾 DB 저장 완료: {}", dto);

            // 7. 응답 생성
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
}

