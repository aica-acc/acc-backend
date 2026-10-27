package com.assistant.acc.service.poster.generate;

import com.assistant.acc.domain.member.UserInputs;
import com.assistant.acc.domain.project.ProposalMetadata;
import com.assistant.acc.dto.create.*;
import com.assistant.acc.dto.create.poster.CreateImageRequestDto;
import com.assistant.acc.dto.create.poster.CreateImageResponseDto;
import com.assistant.acc.dto.create.poster.CreateImageResultResponse;
import com.assistant.acc.dto.create.prompt.GeneratePromptRequestDto;
import com.assistant.acc.dto.create.prompt.GeneratePromptResponseDto;
import com.assistant.acc.dto.create.prompt.GeneratePromptOption;
import com.assistant.acc.dto.create.prompt.SelectedPromptDataDto;
import com.assistant.acc.domain.prompt.Prompt;
import com.assistant.acc.mapper.project.ProjectMapper;
import com.assistant.acc.mapper.project.promotion.GeneratedAssetMapper;
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

import java.io.File;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PromotionAPIServiceImpl implements PromotionAPIService {
    private final UserInputsService userInputsService;
    private final ProjectService proposalMetadataService;
    private final PromptService promptService;
    private final FileStorageService fileStorageService;

    private final PromotionService promotionService;
    private final ProjectMapper projectMapper;
    private final PromptMapper promptMapper;
    private final GeneratedAssetMapper generatedAssetMapper;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // 파일 및 경로
    @Value("${python.api.url}")
    private String pythonApiUrl;

    @Value("${files.generated-root}")
    private String generatedRootDir;

    /**
     * 프롬프트 생성 메서드
     *
     * @param memberNo
     * @param trendData
     * @return
     */
    @Override
    @Transactional
    public List<Prompt> generatePrompts(String memberNo, Map<String, Object> trendData, String promotionType) {
        return generatePrompts(memberNo, null, trendData, promotionType);
    }

    @Override
    @Transactional
    public List<Prompt> generatePrompts(String memberNo, Integer pNo, Map<String, Object> trendData, String promotionType) {
        // 1. 공통 요소 추출
        GenerateElement el = getElement(memberNo, pNo);

        // 2. 중복 생성 방지 (타입별 조회)
        List<Prompt> existingPrompts = promptMapper.selectPromptsByType(
                el.getUserInputs().getUserInputNo(),
                promotionType);

        if (existingPrompts != null && !existingPrompts.isEmpty()) {
            System.out.println("⚠️ [" + promotionType + "] 프롬프트가 이미 존재합니다. 기존 데이터를 반환합니다.");
            return existingPrompts;
        }

        // 3. FastAPI에 전달할 DTO 조립
        System.out.println("3. FastAPI에 전달할 DTO 조립");
        GeneratePromptRequestDto requestDto = new GeneratePromptRequestDto();
        requestDto.setTheme(el.getUserInputs().getTheme());
        requestDto.setAnalysisSummary(el.getProposalMetadata());

        // TODO 실제 트렌드/전략 값 넣기
        requestDto.setPosterTrendReport(trendData);
        requestDto.setStrategyReport(Map.of("strategy", "임시 strategy"));

        // 4. FastAPI 호출
        String endPoint = "/generate-prompt";
        if ("mascot".equals(promotionType)) {
            endPoint = "/generate/mascot/prompt";
        }

        GeneratePromptResponseDto apiResponse = callPython(
                endPoint,
                requestDto,
                GeneratePromptResponseDto.class);
        List<GeneratePromptOption> options = apiResponse.getPromptOptions();

        // 5. prompt 저장
        List<Prompt> saved = new ArrayList<>();

        for (GeneratePromptOption opt : options) {

            String finalPrompt = (opt.getVisualPrompt() != null && !opt.getVisualPrompt().isBlank())
                    ? opt.getVisualPrompt()
                    : opt.getVisualPromptForBackground();

            Prompt prompt = Prompt.builder()
                    .promptNo(null)
                    .userInputNo(el.getUserInputs().getUserInputNo())
                    .visualPrompt(finalPrompt)
                    .styleName(opt.getStyleName())
                    .promotionType(promotionType)
                    .createdAt(LocalDateTime.now())
                    .build();

            saved.add(promptService.savePrompt(prompt));
        }

        return saved;
    }

    /**
     * 생성에 필요한 프로젝트 정보.
     * pNo 를 주면 그 프로젝트, 없으면 회원의 최신 프로젝트 (예전 동작).
     * 백그라운드 작업 스레드에서도 쓰이므로 요청 정보(CurrentMember)를 쓰지 않는다.
     */
    private GenerateElement getElement(String memberNo, Integer requestedPNo) {
        // 프로젝트 번호 로드
        System.out.println("서비스 조회 memberNo" + memberNo);
        Integer pNo = requestedPNo != null ? requestedPNo : projectMapper.selectLatestProjectNo(memberNo);
        if (pNo == null) {
            throw new IllegalStateException("프로젝트가 없습니다. m_no: " + memberNo);
        }

        // 1. user_input 조회
        System.out.println("1. user_input 조회");
        UserInputs ui = userInputsService.getUserInput(pNo);
        if (ui == null) {
            throw new IllegalStateException("user_input 없음 p_no=" + pNo);
        }

        // 2. metadata 조회
        System.out.println("2. metadata 조회");
        // (예전: getLatestProposalMetadata() — 요청의 로그인 정보로 '최신 프로젝트'를 찾아, 작업 스레드에서 실패하고
        //  다른 프로젝트를 이어서 할 때 엉뚱한 기획서를 썼다)
        ProposalMetadata meta = projectMapper.selectProposalMetadata(pNo);
        if (meta == null) {
            throw new IllegalStateException("proposal_metadata 없음 p_no=" + pNo);
        }
        GenerateElement result = GenerateElement.builder().projectNo(pNo).userInputs(ui).proposalMetadata(meta).build();

        return result;
    }

    private <T> T callPython(String endPoint, Object dto, Class<T> responseType) {
        try {
            String url = pythonApiUrl + endPoint;

            String json = objectMapper.writeValueAsString(dto);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> httpEntity = new HttpEntity<>(json, headers);

            ResponseEntity<T> response = restTemplate.postForEntity(url, httpEntity, responseType);

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
    public CreateImageResultResponse createPosterImages(String memberNo, Map<String, Object> trendData,
                                                        String promotionType) {
        return createPosterImages(memberNo, null, trendData, promotionType);
    }

    @Override
    @Transactional
    public CreateImageResultResponse createPosterImages(String memberNo, Integer requestedPNo, Map<String, Object> trendData,
                                                        String promotionType) {
        // 1. 공통 요소 추출
        GenerateElement el = getElement(memberNo, requestedPNo);

        // ⭐ 2-1. DB 체크: 이미 저장된 이미지가 있는지 확인
        if (checkIfImagesExistInDB(el.getProjectNo(), promotionType)) {
            System.out.println("✅ [DB 체크] DB에 " + promotionType + " 이미지 메타데이터가 존재합니다.");

            // 🔍 FileStorageService에 위임
            if (fileStorageService.checkFilesExistInReactPublic(memberNo, el.getProjectNo(), promotionType)) {
                System.out.println("✅ [파일 시스템 체크] React public 폴더에 파일이 존재합니다. 생성을 스킵합니다.");
                return getExistingImagesFromDB(el.getProjectNo(), promotionType);
            } else {
                System.out.println("⚠️ [파일 시스템 체크] React public 폴더에 파일이 없습니다. 파일만 복사합니다.");
                fileStorageService.copyExistingFilesToReact(memberNo, el.getProjectNo(), promotionType);
                return getExistingImagesFromDB(el.getProjectNo(), promotionType);
            }
        }

        // ⭐ 2-2. Python 폴더 체크: 생성되었으나 저장 실패한 이미지 확인 (FileStorageService에 위임)
        if (fileStorageService.checkIfImagesExistInPythonFolder(promotionType)) {
            System.out.println(
                    "⚠️ [Python 폴더 체크] " + promotionType + " 이미지가 Python 폴더에 존재합니다. Python 호출을 스킵하고 기존 이미지를 사용합니다.");
            return processPythonExistingImages(memberNo, el, promotionType);
        }

        // 3. Prompt 조회 (타입별)
        List<Prompt> prompts = promptMapper.selectPromptsByType(
                el.getUserInputs().getUserInputNo(),
                promotionType);

        if (prompts == null || prompts.isEmpty()) {
            throw new IllegalStateException("prompt 없음 user_input_no=" + el.getUserInputs().getUserInputNo());
        }

        // 4. Promotion 생성
        Integer promotionNo = promotionService.createPromotion(
                el.getProjectNo(),
                prompts.get(0).getPromptNo(),
                promotionType);

        // 5. Prompt → SelectedPromptDataDto 변환
        List<SelectedPromptDataDto> selectedList = new ArrayList<>();
        for (Prompt p : prompts) {
            SelectedPromptDataDto dto = new SelectedPromptDataDto();
            dto.setStyleName(p.getStyleName());
            dto.setVisualPrompt(p.getVisualPrompt());
            dto.setWidth(1024);
            dto.setHeight("mascot".equals(promotionType) ? 1024 : 1792);
            dto.setTextContent(null);
            selectedList.add(dto);
        }

        // 6. FastAPI 요청 DTO
        CreateImageRequestDto req = new CreateImageRequestDto();
        req.setAnalysisSummary(el.getProposalMetadata());
        req.setPromptOptions(selectedList);

        // 7. FastAPI 호출 (타입별 엔드포인트)
        System.out.println("🚀 [Python 호출] " + promotionType + " 이미지 생성 시작...");
        String endPoint = "mascot".equals(promotionType)
                ? "/create-mascot-image"
                : "/create-image";

        CreateImageResultResponse result = callPython(endPoint, req, CreateImageResultResponse.class);

        if (result == null || result.getImages() == null) {
            throw new IllegalStateException("이미지 생성 실패 (FastAPI 응답 null)");
        }

        // 8. 파일 이동 + DB 저장
        for (int i = 0; i < result.getImages().size(); i++) {
            CreateImageResponseDto img = result.getImages().get(i);
            // ⭐ 경로에서 파일명만 추출 (마스코트는 /poster-images/mascot/xxx.png 형태이므로)
            String imageUrl = img.getImageUrl().replace("/poster-images/", "");
            String filename = Paths.get(imageUrl).getFileName().toString();
            Integer promptNo = prompts.get(i).getPromptNo();

            fileStorageService.saveGeneratedPosterImage(
                    memberNo,
                    el.getProjectNo(),
                    filename,
                    promptNo,
                    promotionNo,
                    promotionType);
        }

        return result;
    }

    // ============================================
    // 방어 코드 헬퍼 메서드들
    // ============================================

    /**
     * 방어 1: DB에 이미 저장된 이미지가 있는지 확인
     */
    private boolean checkIfImagesExistInDB(Integer projectNo, String promotionType) {
        int count = generatedAssetMapper.countByProjectAndType(projectNo, promotionType);
        return count >= 4;
    }

    /**
     * DB에서 기존 이미지 정보 조회하여 반환
     */
    private CreateImageResultResponse getExistingImagesFromDB(Integer projectNo, String promotionType) {
        CreateImageResultResponse response = new CreateImageResultResponse();
        response.setStatus("success");
        response.setImages(new ArrayList<>());
        return response;
    }

    /**
     * Python 폴더에 있는 이미지를 처리 (DB 저장 재시도)
     * FileStorageService에서 파일 목록 가져오기
     */
    private CreateImageResultResponse processPythonExistingImages(
            String memberNo,
            GenerateElement el,
            String promotionType) {

        // FileStorageService에서 파일 목록 가져오기
        File[] files = fileStorageService.getPythonFolderImages(promotionType);

        List<Prompt> prompts = promptMapper.selectPromptsByType(
                el.getUserInputs().getUserInputNo(),
                promotionType);

        Integer promotionNo = promotionService.createPromotion(
                el.getProjectNo(),
                prompts.get(0).getPromptNo(),
                promotionType);

        List<CreateImageResponseDto> imageResults = new ArrayList<>();

        for (int i = 0; i < Math.min(files.length, prompts.size()); i++) {
            String filename = files[i].getName();
            Integer promptNo = prompts.get(i).getPromptNo();

            fileStorageService.saveGeneratedPosterImage(
                    memberNo,
                    el.getProjectNo(),
                    filename,
                    promptNo,
                    promotionNo,
                    promotionType);

            CreateImageResponseDto dto = new CreateImageResponseDto();
            dto.setFileName(filename);
            dto.setImageUrl("/poster-images/" + filename);
            imageResults.add(dto);
        }

        CreateImageResultResponse response = new CreateImageResultResponse();
        response.setStatus("success");
        response.setImages(imageResults);

        return response;
    }
}