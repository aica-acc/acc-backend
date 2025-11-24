package com.assistant.acc.service.file;

import com.assistant.acc.domain.file.GenerateFilePath;
import com.assistant.acc.domain.project.promotion.GeneratedAsset;
import com.assistant.acc.mapper.file.GenerateFilePathMapper;
import com.assistant.acc.mapper.project.promotion.GeneratedAssetMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    @Value("${files.generated-root}")
    private String baseDir;

    private final GeneratedAssetMapper generatedAssetMapper;
    private final GenerateFilePathMapper generateFilePathMapper;

    private final String PYTHON_SAVE_DIR = "C:/final_project/ACC/acc-ai/홍보물/";

    public void saveGeneratedPosterImage(
            String memberNo,
            Integer projectNo,
            String filename,
            Integer promptNo,
            Integer promotionNo
    ) {
        // ⭐ 1. 저장 폴더 구조 생성
        String targetDir = baseDir + "/" + memberNo + "/" + projectNo + "/poster/";
        File dir = new File(targetDir);
        if (!dir.exists()) {
            dir.mkdirs();   // 구조 전체 자동 생성
        }

        // ⭐ 2. FastAPI가 저장한 기본 폴더 (임시 저장 위치)
        File src = new File("C:/final_project/ACC/acc-ai/홍보물/" + filename);

        // ⭐ 3. 우리의 구조로 이동할 대상 위치
        File dest = new File(targetDir + filename);

        if (src.exists()) {
            src.renameTo(dest);
        } else {
            System.out.println("⚠️ 원본 이미지 없음: " + src.getAbsolutePath());
        }

        // ⭐ 4. generated_asset DB 저장
        GeneratedAsset asset = GeneratedAsset.builder()
                .promotionNo(promotionNo)
                .promptNo(promptNo)
                .isMain(0)                     // 기본은 0
                .generateAssetType("포스터")   // 고정값
                .createdAt(LocalDateTime.now())
                .build();

        generatedAssetMapper.AssetInsert(asset);

        Integer generatedAssetNo = asset.getGeneratedAssetNo();

        // ⭐ 5. generate_file_path DB 저장
        GenerateFilePath path = GenerateFilePath.builder()
                .generatedAssetNo(generatedAssetNo)
                .filePath(dest.getAbsolutePath())
                .fileName(filename)
                .extension("png")
                .build();

        generateFilePathMapper.FilePathInsert(path);
    }
}
