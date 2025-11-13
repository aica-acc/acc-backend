package com.assistant.acc.utility;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class FileSave {

    @Autowired
    private ProposalMapper proposalMapper;

    private static final String BASE_PATH = "C:\\final_project\\ACC\\data\\proposal";

    /**
     * 파일을 저장하고 DB에 메타데이터를 기록
     * @param file 업로드된 파일
     * @param userInputNo 사용자 번호 (없으면 null 가능)
     * @return 저장된 전체 파일 경로
     */
    public String saveFileAndRecord(MultipartFile file, String userInputNo) throws IOException {

        // 1️⃣ 타임스탬프 기반 새 파일명
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")
                .format(LocalDateTime.now());

        // 2️⃣ 원본 파일명
        String originalName = file.getOriginalFilename();

        // 3️⃣ 새 파일명
        String newFileName = timestamp + "_" + originalName;

        // 4️⃣ 디렉토리 생성
        File dir = new File(BASE_PATH);
        if (!dir.exists()) dir.mkdirs();

        // 5️⃣ 파일 저장
        File dest = new File(dir, newFileName);
        Files.copy(file.getInputStream(), dest.toPath());

        // 6️⃣ DB insert
        ProposalFileDTO dto = new ProposalFileDTO();
        dto.setUserInputNo(userInputNo); // null 가능
        dto.setProposalFilePath(dest.getAbsolutePath());
        dto.setProposalFileName(originalName);
        dto.setCreateAt(LocalDateTime.now());

        proposalMapper.insertProposalFile(dto);

        // 7️⃣ 전체 경로 리턴
        return dest.getAbsolutePath();
    }
}
