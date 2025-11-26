package com.assistant.acc.service.poster.regenerate;

import com.assistant.acc.domain.poster.Regenerate;
import com.assistant.acc.domain.poster.RegenerateResponseDTO;
import com.assistant.acc.service.poster.regenerate.utils.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PosterRegenerateServiceImpl implements PosterRegenerateService{

    private final RegenerateFactory factory;
    private final RegeneratePythonService pythonService;
    private final RegenerateFileService fileService;
    private final RegenerateDBService dbService;
    private final RegenerateAssembler assembler;

    @Transactional
    public RegenerateResponseDTO regenerated(String memberNo, Integer filePathNo, String newPrompt) {

        Regenerate regen = factory.create(memberNo, filePathNo, newPrompt);

        // 1. prompt 업데이트
        dbService.updatePrompt(regen);

        // 2. Python 호출
        var pythonImage = pythonService.regenerate(regen);
        regen.setNewImageUrl(pythonImage.getImageUrl());

        // 3. 파일 overwrite
        fileService.overwrite(regen);

        // 4. file_path 업데이트
        dbService.updateFilePath(regen);

        // 5. updated_at 업데이트
        dbService.updateGeneratedAsset(regen);

        // 6. 응답 객체 조립
        return assembler.toResponse(regen);
    }
}