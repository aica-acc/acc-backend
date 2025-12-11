package com.assistant.acc.service.poster.regenerate.utils;

import com.assistant.acc.domain.poster.Regenerate;
import com.assistant.acc.service.file.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RegenerateFileService {

    private final FileStorageService fileStorageService;

    public void overwrite(Regenerate regen, String promotionType) {

        String newFileName = extractFileName(regen.getNewImageUrl());
        regen.setNewFileName(newFileName);

        fileStorageService.overwritePosterImage(
                regen.getMemberNo(),
                regen.getProjectNo(),
                newFileName,
                regen.getOldFileName(),
                promotionType
        );

        String newPath = "/data/promotion/" +
                regen.getMemberNo() + "/" +
                regen.getProjectNo() + "/"+ promotionType + "/" +
                newFileName;

        regen.setNewFilePath(newPath);
    }

    private String extractFileName(String url) {
        return url.substring(url.lastIndexOf("/") + 1);
    }
}