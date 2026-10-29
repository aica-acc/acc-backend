package com.assistant.acc.service.file;

import com.assistant.acc.domain.file.GenerateFilePath;
import com.assistant.acc.domain.project.promotion.GeneratedAsset;
import com.assistant.acc.mapper.file.GenerateFilePathMapper;
import com.assistant.acc.mapper.project.promotion.GeneratedAssetMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    @Value("${react.base-path}")
    private String baseDir;

    private final GeneratedAssetMapper generatedAssetMapper;
    private final GenerateFilePathMapper generateFilePathMapper;
    // AI 결과는 공유 볼륨 경로가 아니라 AI 내부 파일 API 로 받는다 (#21)
    private final AiPromotionFiles aiFiles;

    /**
     * AI 결과를 받아 dest 에 저장하고 AI 쪽 원본을 지운다 (이동). 지우기에 실패하면 받은 파일도 지운다.
     */
    private void moveFromAi(String promotionType, String filename, File dest, String missingMessage) {
        if (!aiFiles.download(promotionType, filename, dest.toPath())) {
            throw new IllegalStateException(missingMessage + promotionType + "/" + filename);
        }
        System.out.println("✅ 파일 복사 성공: " + dest.getAbsolutePath());
        try {
            aiFiles.delete(promotionType, filename);
        } catch (RuntimeException e) {
            // ⚠️ 롤백: 복사된 파일도 삭제하고 예외 던지기
            dest.delete();
            throw new RuntimeException("원본 파일 삭제 실패, 전체 작업 롤백: " + promotionType + "/" + filename, e);
        }
        System.out.println("✅ 원본 파일 삭제 성공, 이동 완료: " + promotionType + "/" + filename);
    }

    public void saveGeneratedPosterImage(
            String memberNo,
            Integer projectNo,
            String filename,
            Integer promptNo,
            Integer promotionNo,
            String promotionType) {
        // ⭐ 1. React public 폴더의 저장 경로 정의
        String targetDir = Paths.get(
                baseDir,
                "public",
                "data",
                "promotion",
                memberNo,
                projectNo.toString(),
                promotionType).toString();

        File dir = new File(targetDir);
        if (!dir.exists()) {
            dir.mkdirs(); // 구조 전체 자동 생성
        }

        // ⭐ 2. AI 결과 이동 (받기 + 원본 삭제, 실패 시 롤백)
        File dest = new File(Paths.get(targetDir, filename).toString());
        moveFromAi(promotionType, filename, dest, "원본 이미지 파일이 존재하지 않습니다: ");

        // ⭐ 3. generated_asset DB 저장
        GeneratedAsset asset = GeneratedAsset.builder()
                .promotionNo(promotionNo)
                .promptNo(promptNo)
                .isMain(0) // 기본은 0
                .generateAssetType(promotionType) // 타입 저장
                .createdAt(LocalDateTime.now())
                .build();

        generatedAssetMapper.AssetInsert(asset);
        Integer generatedAssetNo = asset.getGeneratedAssetNo();

        // ⭐ 4. generate_file_path DB 저장
        String dbPath = "/data/promotion/" + memberNo + "/" + projectNo + "/" + promotionType + "/" + filename;

        GenerateFilePath path = GenerateFilePath.builder()
                .generatedAssetNo(generatedAssetNo)
                .filePath(dbPath)
                .fileName(filename)
                .extension("png")
                .build();

        generateFilePathMapper.FilePathInsert(path);
    }

    public void overwritePosterImage(
            String memberNo,
            Integer projectNo,
            String newFilename,
            String oldFilename,
            String promotionType) {

        String targetDir = Paths.get(
                baseDir,
                "public",
                "data",
                "promotion",
                memberNo,
                projectNo.toString(),
                promotionType).toString();

        // 경로 보장
        File dir = new File(targetDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        /* ⭐ (1) 기존 파일 삭제 */
        File oldFile = new File(targetDir + File.separator + oldFilename);
        if (oldFile.exists() && !oldFile.delete()) {
            System.out.println("⚠️ 기존 파일 삭제 실패: " + oldFile.getAbsolutePath());
        }

        /* ⭐ (2) AI 에서 새 파일 이동 + 롤백 */
        File dest = new File(targetDir + File.separator + newFilename);
        moveFromAi(promotionType, newFilename, dest, "재생성된 이미지 파일이 존재하지 않습니다: ");
    }

    /**
     * React public 폴더에 실제 파일이 존재하는지 확인
     */
    public boolean checkFilesExistInReactPublic(String memberNo, Integer projectNo, String promotionType) {
        List<GeneratedAsset> assets = generatedAssetMapper.selectByProjectAndType(projectNo, promotionType);

        if (assets == null || assets.isEmpty()) {
            return false;
        }

        String targetDir = Paths.get(
                baseDir,
                "public",
                "data",
                "promotion",
                memberNo,
                projectNo.toString(),
                promotionType
        ).toString();

        File dir = new File(targetDir);

        if (!dir.exists()) {
            System.out.println("  📁 React public 디렉토리가 존재하지 않음: " + dir.getAbsolutePath());
            return false;
        }

        File[] files = dir.listFiles((d, name) -> name.endsWith(".png"));

        if (files == null || files.length < 4) {
            System.out.println("  📁 React public에 파일 " + (files != null ? files.length : 0) + "개 발견 (필요: 4개)");
            return false;
        }

        System.out.println("  ✅ React public에 " + files.length + "개 파일 확인됨");
        return true;
    }

    /**
     * AI 폴더에서 React public으로 파일만 복사 (DB 저장 없음)
     */
    public void copyExistingFilesToReact(String memberNo, Integer projectNo, String promotionType) {
        List<String> files = aiFiles.list(promotionType);

        if (files.isEmpty()) {
            throw new IllegalStateException("AI 폴더에 파일이 없습니다: " + promotionType);
        }

        System.out.println("🔄 [파일 복사 모드] AI → React public 복사 시작...");

        String targetDir = Paths.get(
                baseDir,
                "public",
                "data",
                "promotion",
                memberNo,
                projectNo.toString(),
                promotionType
        ).toString();
        new File(targetDir).mkdirs();

        for (String filename : files) {
            System.out.println("  📋 파일 복사 시작: " + filename);
            if (!aiFiles.download(promotionType, filename, Paths.get(targetDir, filename))) {
                System.out.println("  ⚠️ AI 파일 없음: " + promotionType + "/" + filename);
            }
        }

        System.out.println("✅ [파일 복사 완료] " + files.size() + "개 파일이 React public으로 복사되었습니다.");
    }

    /**
     * AI 폴더에 이미 생성된 이미지가 있는지 확인
     */
    public boolean checkIfImagesExistInPythonFolder(String promotionType) {
        List<String> files = aiFiles.list(promotionType);

        if (files.size() >= 4) {
            System.out.println("  📁 AI 폴더에 " + promotionType + " 이미지 " + files.size() + "개 발견");
            return true;
        }

        return false;
    }

    /**
     * AI 폴더의 파일명 목록. AI 가 파일명({type}_{시각}_{순번}.png) 순으로 정렬해 주므로
     * 호출부에서 프롬프트 순서와 짝이 맞는다.
     */
    public List<String> getPythonFolderImages(String promotionType) {
        List<String> files = aiFiles.list(promotionType);

        if (files.isEmpty()) {
            throw new IllegalStateException("AI 폴더에 " + promotionType + " 이미지가 없습니다.");
        }

        return files;
    }
}
