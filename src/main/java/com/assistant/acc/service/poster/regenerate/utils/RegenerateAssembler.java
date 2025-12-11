package com.assistant.acc.service.poster.regenerate.utils;

import com.assistant.acc.domain.poster.Regenerate;
import com.assistant.acc.domain.poster.RegenerateResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class RegenerateAssembler {

    public RegenerateResponseDTO toResponse(Regenerate regen) {
        return RegenerateResponseDTO.builder()
                .filePathNo(regen.getFilePathNo())
                .promptNo(regen.getPromptNo())
                .fileUrl(regen.getNewFilePath())
                .fileName(regen.getNewFileName())
                .extension("png")
                .visualPrompt(regen.getNewPrompt())
                .styleName(regen.getStyleName())
                .newImageUrl(regen.getNewImageUrl())
                .success(true)
                .build();
    }
}