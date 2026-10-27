package com.assistant.acc.service.file;

import com.assistant.acc.domain.file.GenerateFilePath;
import com.assistant.acc.domain.project.promotion.GeneratedAsset;
import com.assistant.acc.mapper.file.GenerateFilePathMapper;
import com.assistant.acc.mapper.project.promotion.GeneratedAssetMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    @Value("${react.base-path}")
    private String baseDir;

    @Value("${python.base-dir}")
    private String pythonPromotionBaseDir;

    private final GeneratedAssetMapper generatedAssetMapper;
    private final GenerateFilePathMapper generateFilePathMapper;

    /**
     * 프로모션 타입에 따른 Python 원본 경로 생성
     */
    private String getPythonSourceDir(String promotionType) {
        return Paths.get(pythonPromotionBaseDir, promotionType).toString();
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

        // ⭐ 2. FastAPI가 저장한 기본 폴더 (타입별 경로)
        String pythonSourceDir = getPythonSourceDir(promotionType);
        String pythonSourcePath = Paths.get(pythonSourceDir, filename).toString();

        File src = new File(pythonSourcePath);
        File dest = new File(Paths.get(targetDir, filename).toString());

        // ⭐ 3. 파일 복사 + 예외 처리 + 롤백
        if (!src.exists()) {
            throw new IllegalStateException("원본 이미지 파일이 존재하지 않습니다: " + src.getAbsolutePath());
        }

        try {
            // renameTo 대신 Files.copy 사용 (더 안정적)
            Files.copy(
                    src.toPath(),
                    dest.toPath(),
                    StandardCopyOption.REPLACE_EXISTING);

            System.out.println("✅ 파일 복사 성공: " + dest.getAbsolutePath());

            // 원본 파일 삭제 (이동 완료)
            if (!src.delete()) {
                // ⚠️ 롤백: 복사된 파일도 삭제하고 예외 던지기
                dest.delete();
                throw new RuntimeException("원본 파일 삭제 실패, 전체 작업 롤백: " + src.getAbsolutePath());
            }

            System.out.println("✅ 원본 파일 삭제 성공, 이동 완료: " + src.getAbsolutePath());

        } catch (IOException e) {
            throw new RuntimeException("파일 복사 실패: " + src.getAbsolutePath() + " → " + dest.getAbsolutePath(), e);
        }

        // ⭐ 4. generated_asset DB 저장
        GeneratedAsset asset = GeneratedAsset.builder()
                .promotionNo(promotionNo)
                .promptNo(promptNo)
                .isMain(0) // 기본은 0
                .generateAssetType(promotionType) // 타입 저장
                .createdAt(LocalDateTime.now())
                .build();

        generatedAssetMapper.AssetInsert(asset);
        Integer generatedAssetNo = asset.getGeneratedAssetNo();

        // ⭐ 5. generate_file_path DB 저장
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

        /* ⭐ (1) Python 생성 폴더에서 새 파일 가져오기 */
        String pythonSourceDir = getPythonSourceDir(promotionType);
        String pythonSourcePath = Paths.get(pythonSourceDir, newFilename).toString();

        File src = new File(pythonSourcePath);

        if (!src.exists()) {
            throw new IllegalStateException("재생성된 이미지 파일이 존재하지 않습니다: " + src.getAbsolutePath());
        }

        /* ⭐ (2) 기존 파일 삭제 */
        File oldFile = new File(targetDir + File.separator + oldFilename);
        if (oldFile.exists() && !oldFile.delete()) {
            System.out.println("⚠️ 기존 파일 삭제 실패: " + oldFile.getAbsolutePath());
        }

        /* ⭐ (3) 새 파일 복사 + 롤백 */
        File dest = new File(targetDir + File.separator + newFilename);
        try {
            Files.copy(
                    src.toPath(),
                    dest.toPath(),
                    StandardCopyOption.REPLACE_EXISTING);

            System.out.println("✅ 재생성 파일 복사 성공: " + dest.getAbsolutePath());

            // 원본 파일 삭제 (이동 완료)
            if (!src.delete()) {
                // ⚠️ 롤백: 복사된 파일도 삭제하고 예외 던지기
                dest.delete();
                throw new RuntimeException("재생성 원본 파일 삭제 실패, 전체 작업 롤백: " + src.getAbsolutePath());
            }

            System.out.println("✅ 재생성 원본 파일 삭제 성공, 이동 완료: " + src.getAbsolutePath());

        } catch (IOException e) {
            throw new RuntimeException("재생성 파일 복사 실패: " + src.getAbsolutePath() + " → " + dest.getAbsolutePath(), e);
        }

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
     * Python 폴더에서 React public으로 파일만 복사 (DB 저장 없음)
     */
    public void copyExistingFilesToReact(String memberNo, Integer projectNo, String promotionType) {
        String pythonDir = getPythonSourceDir(promotionType);
        String filePrefix = promotionType + "_";

        File dir = new File(pythonDir);
        File[] files = dir.listFiles((d, name) ->
                name.startsWith(filePrefix) && name.endsWith(".png")
        );

        if (files == null || files.length == 0) {
            throw new IllegalStateException("Python 폴더에 파일이 없습니다: " + pythonDir);
        }

        System.out.println("🔄 [파일 복사 모드] Python → React public 복사 시작...");

        for (File file : files) {
            String filename = file.getName();
            System.out.println("  📋 파일 복사 시작: " + filename);
            copyFileOnlyWithoutDB(memberNo, projectNo, filename, promotionType);
        }

        System.out.println("✅ [파일 복사 완료] " + files.length + "개 파일이 React public으로 복사되었습니다.");
    }

    /**
     * DB 저장 없이 파일만 복사 (private helper)
     */
    private void copyFileOnlyWithoutDB(String memberNo, Integer projectNo, String filename, String promotionType) {
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
            dir.mkdirs();
        }

        String pythonSourceDir = getPythonSourceDir(promotionType);
        String pythonSourcePath = Paths.get(pythonSourceDir, filename).toString();

        File src = new File(pythonSourcePath);
        File dest = new File(Paths.get(targetDir, filename).toString());

        if (!src.exists()) {
            System.out.println("  ⚠️ Python 파일 없음: " + src.getAbsolutePath());
            return;
        }

        try {
            Files.copy(
                    src.toPath(),
                    dest.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
            );
            System.out.println("  ✅ 파일 복사 성공: " + dest.getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("파일 복사 실패: " + filename, e);
        }
    }

    /**
     * Python 폴더에 이미 생성된 이미지가 있는지 확인
     */
    public boolean checkIfImagesExistInPythonFolder(String promotionType) {
        String pythonDir = getPythonSourceDir(promotionType);
        String filePrefix = promotionType + "_";

        File dir = new File(pythonDir);

        if (!dir.exists()) {
            return false;
        }

        File[] files = dir.listFiles((d, name) -> name.startsWith(filePrefix) && name.endsWith(".png"));

        if (files != null && files.length >= 4) {
            System.out.println("  📁 Python 폴더에 " + promotionType + " 이미지 " + files.length + "개 발견");
            return true;
        }

        return false;
    }

    /**
     * Python 폴더에서 파일 목록 가져오기
     */
    public File[] getPythonFolderImages(String promotionType) {
        String pythonDir = getPythonSourceDir(promotionType);
        String filePrefix = promotionType + "_";

        File dir = new File(pythonDir);

        if (!dir.exists()) {
            throw new IllegalStateException("Python 폴더가 존재하지 않습니다: " + pythonDir);
        }

        File[] files = dir.listFiles((d, name) -> name.startsWith(filePrefix) && name.endsWith(".png"));

        if (files == null || files.length == 0) {
            throw new IllegalStateException("Python 폴더에 " + promotionType + " 이미지가 없습니다.");
        }

        // listFiles 는 순서를 보장하지 않는다. 파일명({type}_{시각}_{순번}.png) 순으로 정렬해야
        // 호출부에서 프롬프트 순서와 짝이 맞는다.
        java.util.Arrays.sort(files, java.util.Comparator.comparing(File::getName));
        return files;
    }
}