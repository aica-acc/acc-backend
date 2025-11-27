package com.assistant.acc.service.poster.regenerate.utils;

import com.assistant.acc.domain.poster.Regenerate;
import com.assistant.acc.mapper.poster.regenerate.RegenerateMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RegenerateDBService {

    private final RegenerateMapper mapper;

    public void updatePrompt(Regenerate regen) {
        mapper.updatePromptVisual(regen.getPromptNo(), regen.getNewPrompt());
    }

    public void updateFilePath(Regenerate regen) {
        mapper.updateFilePath(
                regen.getFilePathNo(),
                regen.getNewFilePath(),
                regen.getNewFileName(),
                "png"
        );
    }

    public void updateGeneratedAsset(Regenerate regen) {
        mapper.touchGeneratedAssetUpdateAt(regen.getGeneratedAssetNo());
    }
}