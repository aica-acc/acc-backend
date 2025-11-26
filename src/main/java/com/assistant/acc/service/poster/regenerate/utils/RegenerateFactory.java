package com.assistant.acc.service.poster.regenerate.utils;

import com.assistant.acc.domain.poster.Regenerate;
import com.assistant.acc.domain.poster.RegenerateAssetDetail;
import com.assistant.acc.mapper.poster.regenerate.RegenerateMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RegenerateFactory {

    private final RegenerateMapper mapper;

    public Regenerate create(String memberNo, Integer filePathNo, String newPrompt) {

        RegenerateAssetDetail detail = mapper.getRegenerateAssetDetail(filePathNo);

        if (detail == null) {
            throw new IllegalArgumentException("파일을 찾을 수 없습니다. filePathNo=" + filePathNo);
        }

        if (!detail.getMemberNo().equals(memberNo)) {
            throw new SecurityException("본인 자산만 수정할 수 있습니다.");
        }
        System.out.println("재생성 이미지 정보" + detail);


        return Regenerate.builder()
                .memberNo(memberNo)

                .filePathNo(filePathNo)
                .promptNo(detail.getPromptNo())
                .generatedAssetNo(detail.getGeneratedAssetNo())
                .promotionNo(detail.getPromotionNo())
                .projectNo(detail.getProjectNo())

                .oldFileName(detail.getFileName())
                .oldFilePath(detail.getFileUrl())

                .styleName(detail.getStyleName())
                .oldPrompt(detail.getVisualPrompt())

                .newPrompt(newPrompt)
                .build();
    }
}
