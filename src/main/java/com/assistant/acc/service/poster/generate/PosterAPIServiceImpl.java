package com.assistant.acc.service.poster.generate;

import com.assistant.acc.domain.member.UserInputs;
import com.assistant.acc.domain.project.ProposalMetadata;
import com.assistant.acc.domain.prompt.Prompt;
import com.assistant.acc.dto.create.*;
import com.assistant.acc.dto.create.poster.CreateImageRequestDto;
import com.assistant.acc.dto.create.poster.CreateImageResponseDto;
import com.assistant.acc.dto.create.poster.CreateImageResultResponse;
import com.assistant.acc.dto.create.prompt.CreatePromptRequestDto;
import com.assistant.acc.dto.create.prompt.CreatePromptResponseDto;
import com.assistant.acc.dto.create.prompt.PosterPromptOption;
import com.assistant.acc.dto.create.prompt.SelectedPromptDataDto;
import com.assistant.acc.mapper.project.ProjectMapper;
import com.assistant.acc.mapper.prompt.PromptMapper;
import com.assistant.acc.service.file.FileStorageService;
import com.assistant.acc.service.member.UserInputsService;
import com.assistant.acc.service.project.ProjectService;
import com.assistant.acc.service.project.promotion.PromotionService;
import com.assistant.acc.service.prompt.PromptService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PosterAPIServiceImpl implements PosterAPIService {
    private final UserInputsService userInputsService;
    private final ProjectService proposalMetadataService;
    private final PromptService promptService;
    private final FileStorageService fileStorageService;

    private final PromotionService promotionService;
    private final ProjectMapper projectMapper;
    private final PromptMapper promptMapper;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // 파일 및 경로
    @Value("${python.api.url}")
    private String pythonApiUrl;    // 파이썬 통신용 URI

    @Value("${files.generated-root}")
    private String generatedRootDir;    // properties 추가

    /**
     * 프롬프트 생성 메서드
     * @param memberNo
     * @param trendData
     * @return
     */
    @Override
    @Transactional
    public List<Prompt> generatePrompts(String memberNo, Map<String, Object> trendData) {
        // 1. 공통 요소 추출
        GenerateElement el = getElement(memberNo);

        // 2. FastAPI에 전달할 DTO 조립
        System.out.println("3. FastAPI에 전달할 DTO 조립");
        CreatePromptRequestDto requestDto = new CreatePromptRequestDto();
        requestDto.setTheme(el.getUserInputs().getTheme());
        requestDto.setAnalysisSummary(el.getProposalMetadata());

        // TODO 실제 트렌드/전략 값 넣기
        requestDto.setPosterTrendReport(trendData);
        requestDto.setStrategyReport(Map.of("strategy", "임시 strategy"));

        // 3. FastAPI 호출
        CreatePromptResponseDto apiResponse = callPython(
                "/generate-prompt",
                requestDto,
                CreatePromptResponseDto.class
        );
        List<PosterPromptOption> options = apiResponse.getPromptOptions();

        // 4. prompt 저장
        List<Prompt> saved = new ArrayList<>();

        for (PosterPromptOption opt : options) {

            String finalPrompt =
                    (opt.getVisualPrompt() != null && !opt.getVisualPrompt().isBlank())
                            ? opt.getVisualPrompt()
                            : opt.getVisualPromptForBackground();

            Prompt prompt = Prompt.builder()
                    .promptNo(null)
                    .userInputNo(el.getUserInputs().getUserInputNo())
                    .visualPrompt(finalPrompt)
                    .styleName(opt.getStyleName())
                    .createdAt(LocalDateTime.now())
                    .build();

            saved.add(promptService.savePrompt(prompt));
        }

        return saved;
    }

    private GenerateElement getElement(String memberNo){
        // 프로젝트 번호 로드
        System.out.println("서비스 조회 memberNo" + memberNo);
        Integer pNo = projectMapper.selectLatestProjectNo(memberNo);
        if(pNo == null) { throw new IllegalStateException("프로젝트가 없습니다. m_no: " + memberNo);}

        // 1. user_input 조회
        System.out.println("1. user_input 조회");
        UserInputs ui = userInputsService.getUserInput(pNo);
        if (ui == null) {
            throw new IllegalStateException("user_input 없음 p_no=" + pNo);
        }

        // 2. metadata 조회
        System.out.println("2. metadata 조회");
        ProposalMetadata meta = proposalMetadataService.getLatestProposalMetadata();
        if (meta == null) {
            throw new IllegalStateException("proposal_metadata 없음 p_no=" + pNo);
        }
        GenerateElement result = GenerateElement.builder().
                projectNo(pNo).userInputs(ui).proposalMetadata(meta).build();

        return result;
    }

    private <T > T callPython(String endPoint, Object dto, Class<T> responseType) {
        try {
            String url = pythonApiUrl + endPoint;

            String json = objectMapper.writeValueAsString(dto);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> httpEntity = new HttpEntity<>(json, headers);

            ResponseEntity<T> response =
                    restTemplate.postForEntity(url, httpEntity, responseType);

            return response.getBody();

        } catch (Exception e) {
            throw new RuntimeException("파이썬 호출 실패: " + endPoint, e);
        }
    }

    /**
     * 이미지 생성
     */

    @Override
    @Transactional
    public CreateImageResultResponse createPosterImages(String memberNo, Map<String, Object> trendData) {
        // 1. 공통 요소 추출
        GenerateElement el = getElement(memberNo);

        // 2. prompt 4개 조회
        List<Prompt> prompts = promptMapper.selectPrompts(el.getUserInputs().getUserInputNo());
        if (prompts == null || prompts.isEmpty()) {
            throw new IllegalStateException("prompt 없음 user_input_no=" + el.getUserInputs().getUserInputNo());
        }

        Integer promotionNo = promotionService.createPromotion(
                el.getProjectNo(),
                prompts.get(0).getPromptNo(),
                "포스터"
        );

        // 3. Prompt → SelectedPromptDataDto 변환
        List<SelectedPromptDataDto> selectedList = new ArrayList<>();
        for (Prompt p : prompts) {
            SelectedPromptDataDto dto = new SelectedPromptDataDto();
            dto.setStyleName(p.getStyleName());
            dto.setVisualPrompt(p.getVisualPrompt());
            dto.setWidth(1024);
            dto.setHeight(1792);
            dto.setTextContent(null); // 현재 text_content 없음
            selectedList.add(dto);
        }

        // 6. FastAPI 요청 DTO 만들기
        CreateImageRequestDto req = new CreateImageRequestDto();
        req.setAnalysisSummary(el.getProposalMetadata());
        req.setPromptOptions(selectedList);

        // 7. FastAPI 호출
        CreateImageResultResponse result = callPython(
                "/create-image",
                req,
                CreateImageResultResponse.class
        );

        if (result == null || result.getImages() == null) {
            throw new IllegalStateException("이미지 생성 실패 (FastAPI 응답 null)");
        }

        // 6. *** 파일 이동 + DB 저장은 FileStorageService 로 분리 ***
        for (int i = 0; i < result.getImages().size(); i++) {

            CreateImageResponseDto img = result.getImages().get(i);

            String filename = img.getImageUrl().replace("/poster-images/", "");

            Integer promptNo = prompts.get(i).getPromptNo();

            fileStorageService.saveGeneratedPosterImage(
                    memberNo,
                    el.getProjectNo(),
                    filename,
                    promptNo,
                    promotionNo     // ⭐ promotionNo 넘겨줘야 함
            );
        }

        return result;
    }
}