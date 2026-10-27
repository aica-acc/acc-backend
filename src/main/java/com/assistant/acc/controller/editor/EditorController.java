package com.assistant.acc.controller.editor;

import com.assistant.acc.domain.editor.EditorTemplate;
import com.assistant.acc.dto.editor.EditorAiRenderRequest;
import com.assistant.acc.dto.editor.EditorAiRenderResponse;
import com.assistant.acc.dto.editor.EditorBuildResponse;
import com.assistant.acc.dto.editor.SaveImageRequest;
import com.assistant.acc.dto.editor.SaveImageResponse;
import com.assistant.acc.service.editor.EditorAiRenderService;
import com.assistant.acc.service.editor.EditorBuildService;
import com.assistant.acc.service.editor.EditorImageSaveService;
import com.assistant.acc.service.editor.EditorTemplateService;
import com.assistant.acc.security.ProjectAccess;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/editor")
@RequiredArgsConstructor
public class EditorController {

    private final EditorBuildService editorBuildService;
    private final EditorTemplateService editorTemplateService;
    private final EditorAiRenderService editorAiRenderService;
    private final EditorImageSaveService editorImageSaveService;
    private final ProjectAccess projectAccess;

    // ==============================
    // 1) 템플릿 빌드
    // ==============================
    /**
     * 🎨 에디터용 템플릿 빌드 트리거
     *
     * FE → POST /api/editor/build?pNo=40
     * Body(JSON 예시):
     * [
     *   {
     *     "posterImageUrl": "C:/.../original_poster_1.png",
     *     "title": "담양 산타 축제",
     *     "festivalStartDate": "2025-12-24",
     *     "festivalEndDate": "2025-12-25",
     *     "location": "메타랜드 일원",
     *     "types": ["road_banner", "bus_road"]
     *   }
     * ]
     *
     * Controller:
     *  - pNo: 쿼리 파라미터로 받음
     *  - postersJson: request body 전체를 raw JSON String 으로 받음
     *
     * Service:
     *  - postersJson → List<Map<String, Object>> 로 파싱
     *  - Python /editor/build 호출
     *  - Python 응답의 filePath 들을 editor_template 테이블에 저장
     */
        @PostMapping("/build")
        public ResponseEntity<EditorBuildResponse> buildTemplates(
                @RequestParam("pNo") Integer pNo,
                @RequestBody String postersJson
        ) {
        log.info("🎨 [EditorController] /build pNo={}, rawPostersJson.length={}",
                pNo, postersJson != null ? postersJson.length() : 0);

        projectAccess.check(pNo);
        EditorBuildResponse response = editorBuildService.buildAndSaveTemplates(pNo, postersJson);
        return ResponseEntity.ok(response);
        }
    // ==============================
    // 2) 템플릿 목록 조회
    // ==============================
    /**
     * 🔍 특정 프로젝트(pNo)의 템플릿 목록 조회
     *  - editor_template 테이블에서 file_path 목록 가져오기
     *  - 프론트 에디터에서 "작업물 리스트" 띄울 때 사용
     *
     * 예시:
     *   GET /api/editor/templates?pNo=40
     */
        @GetMapping("/project/{pNo}/template-json")
        public ResponseEntity<Map<String, Object>> getTemplateJson(@PathVariable Integer pNo)  {

        log.info("📥 [EditorController] GET /template-json pNo={}", pNo);

        // 1) 최신 템플릿 row 가져오기
        projectAccess.check(pNo);
        EditorTemplate template = editorTemplateService.getLatestTemplate(pNo);
        if (template == null) {
                return ResponseEntity.notFound().build();
        }

        String filePath = template.getFilePath();
        log.info("📄 Using filePath={}", filePath);

        // 2) 파일 열어서 JSON 배열 로딩
        List<Map<String, Object>> items = editorTemplateService.loadTemplateJson(filePath);

        // 3) 응답 구조: { pNo, items }
        Map<String, Object> response = new HashMap<>();
        response.put("pNo", pNo);
        response.put("items", items);

        return ResponseEntity.ok(response);
        }

    // ==============================
    // 3) AI 색상 추천 (스타일링)
    // ==============================
    /**
     * 🎨 AI를 통한 캔버스 텍스트 스타일 추천 (색상, 폰트 등)
     *
     * FE:
     *   - backgroundImageUrl: 배경 이미지 URL
     *   - canvasJson: 현재 캔버스 데이터 (Fabric.js JSON)
     *   - layoutType: 레이아웃 타입 (카테고리명)
     *
     * BE:
     *   - AI 서버(/editor/render)로 요청 전달
     *   - 변경된 canvasData 반환 (변경 가능한 스타일 필드만 수정됨)
     */
    @PostMapping("/ai-render")
    public ResponseEntity<EditorAiRenderResponse> renderWithAi(
            @RequestBody EditorAiRenderRequest request
    ) {
        log.info("🎨 [EditorController] /ai-render layoutType={}",
                request.getLayoutType());

        EditorAiRenderResponse res = editorAiRenderService.renderWithAi(request);
        return ResponseEntity.ok(res);
    }

    // ==============================
    // 4) 에디터 이미지 저장
    // ==============================
    /**
     * 💾 에디터에서 수정한 이미지를 서버에 저장하고 DB에 저장
     *
     * FE:
     *   - pNo: 프로젝트 번호
     *   - imageBase64: base64 이미지 데이터 (data:image/png;base64,...)
     *   - dbFileType: 파일 타입 ("poster", "mascot", "banner" 등)
     *
     * BE:
     *   - base64 디코딩
     *   - PUBLIC_FOLDER_PATH/data/promotion/m000001/{pNo}/editor/ 경로에 저장
     *   - promotion_path 테이블에 저장
     *   - 저장된 경로 반환
     */
    @PostMapping("/save-image")
    public ResponseEntity<SaveImageResponse> saveImage(
            @RequestBody SaveImageRequest request
    ) {
        log.info("💾 [EditorController] /save-image pNo={}, dbFileType={}",
                request.getPNo(), request.getDbFileType());

        projectAccess.check(request.getPNo());
        SaveImageResponse response = editorImageSaveService.saveEditorImage(
                request.getPNo(),
                request.getImageBase64(),
                request.getImagePath(),
                request.getDbFileType()
        );

        if (response.getSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
}
