package com.assistant.acc.service.poster.regenerate.utils;

import com.assistant.acc.domain.poster.Regenerate;
import com.assistant.acc.domain.project.ProposalMetadata;
import com.assistant.acc.dto.create.poster.CreateImageRequestDto;
import com.assistant.acc.dto.create.poster.CreateImageResponseDto;
import com.assistant.acc.dto.create.poster.CreateImageResultResponse;
import com.assistant.acc.dto.create.prompt.SelectedPromptDataDto;
import com.assistant.acc.mapper.project.ProjectMapper;
import com.assistant.acc.utility.ProposalMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RegeneratePythonService {

    private final RestTemplate restTemplate;
    private final ProjectMapper projectMapper;

    public CreateImageResponseDto regenerate(Regenerate regen) {

        // 1. p_no 조회
        Integer pNo = projectMapper.selectLatestProjectNo(regen.getMemberNo());

        // 2. 메타데이터 조회
        ProposalMetadata meta = projectMapper.selectProposalMetadata(pNo);

        // 3. prompt 조립
        SelectedPromptDataDto selected = new SelectedPromptDataDto();
        selected.setStyleName(regen.getStyleName());
        selected.setVisualPrompt(regen.getNewPrompt());
        selected.setWidth(1024);
        selected.setHeight(1792);
        selected.setTextContent(null);

        // 4. create-image 요청 DTO 생성
        CreateImageRequestDto req = new CreateImageRequestDto();
        req.setAnalysisSummary(meta);
        req.setPromptOptions(List.of(selected));

        // 5. Python 호출
        CreateImageResultResponse response = restTemplate.postForObject(
                "http://127.0.0.1:5000/create-image",
                req,
                CreateImageResultResponse.class
        );

        return response.getImages().get(0);  // regenerate는 단건
    }
}
