package com.assistant.acc.service.poster.regenerate.utils;

import org.springframework.beans.factory.annotation.Value;
import com.assistant.acc.domain.poster.Regenerate;
import com.assistant.acc.domain.project.ProposalMetadata;
import com.assistant.acc.dto.create.poster.CreateImageRequestDto;
import com.assistant.acc.dto.create.poster.CreateImageResponseDto;
import com.assistant.acc.dto.create.poster.CreateImageResultResponse;
import com.assistant.acc.dto.create.prompt.SelectedPromptDataDto;
import com.assistant.acc.mapper.project.ProjectMapper;
import com.assistant.acc.service.file.AiPromotionFiles;
import com.assistant.acc.utility.ProposalMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RegeneratePythonService {

    @Value("${python.api.url:http://localhost:5000}")
    private String pythonApiUrl;

    private final RestTemplate restTemplate;
    private final ProjectMapper projectMapper;

    public CreateImageResponseDto regenerate(Regenerate regen, String promotionType) {

        // 1. 고른 이미지의 프로젝트 (예전에는 회원의 "가장 최근 프로젝트" 라 다른 프로젝트 기획서로 다시 만들어졌다, #26 과 같은 유형)
        Integer pNo = regen.getProjectNo();
        boolean mascot = "mascot".equals(promotionType);

        // 2. 메타데이터 조회
        ProposalMetadata meta = projectMapper.selectProposalMetadata(pNo);

        // 3. prompt 조립
        SelectedPromptDataDto selected = new SelectedPromptDataDto();
        selected.setStyleName(regen.getStyleName());
        selected.setVisualPrompt(regen.getNewPrompt());
        selected.setWidth(1024);
        selected.setHeight(mascot ? 1024 : 1792);
        selected.setTextContent(null);

        // 4. create-image 요청 DTO 생성
        CreateImageRequestDto req = new CreateImageRequestDto();
        req.setAnalysisSummary(meta);
        req.setPromptOptions(List.of(selected));
        // 다시 만들기는 같은 프롬프트라도 새 그림이어야 하므로 요청마다 다른 폴더 (#31)
        regen.setBatch(AiPromotionFiles.batchOf(regen.getMemberNo(), pNo) + "_r" + System.currentTimeMillis());
        req.setBatch(regen.getBatch());

        // 5. Python 호출 (마스코트는 마스코트 엔드포인트)
        CreateImageResultResponse response = restTemplate.postForObject(
                pythonApiUrl + (mascot ? "/create-mascot-image" : "/create-image"),
                req,
                CreateImageResultResponse.class
        );

        return response.getImages().get(0);  // regenerate는 단건
    }
}
