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
import com.assistant.acc.security.CurrentMember;
import com.assistant.acc.security.ProjectAccess;
import com.assistant.acc.service.credit.CreditService;
import com.assistant.acc.service.credit.CreditService.Charge;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.assistant.acc.service.job.GenerationJobService;
import com.assistant.acc.service.job.GenerationJobService.Job;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
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
    private final GenerationJobService jobService;
    private final RestTemplate restTemplate;
    private final CreditService creditService;
    private final ObjectMapper objectMapper;

    @Value("${python.api.url:http://localhost:5000}")
    private String pythonApiUrl;

    // ==============================
    // 0) 파생물 생성(템플릿 빌드)을 백그라운드 작업으로 시작
    // ==============================
    /**
     * POST /api/editor/build-jobs?pNo=40  (body: postersPayload JSON 문자열, /build 와 같음)
     * 바로 작업 번호를 돌려준다. 진행 상황은 GET /api/jobs/{jobId}.
     * 같은 프로젝트의 빌드가 이미 돌고 있으면 그 작업을 돌려준다.
     */
    @PostMapping("/build-jobs")
    public Job startBuildJob(@RequestParam("pNo") Integer pNo, @RequestBody String postersJson) {
        projectAccess.check(pNo);
        Job running = jobService.active(pNo, GenerationJobService.EDITOR_BUILD);
        if (running != null) {
            return running;
        }
        boolean demo = CurrentMember.isTest();
        Charge charge = demo ? null : chargeBuild(pNo, postersJson);
        return jobService.start(pNo, CurrentMember.no(), GenerationJobService.EDITOR_BUILD,
                progress -> editorBuildService.buildAndSaveTemplates(pNo, postersJson, demo),
                charge == null ? null : charge::refund);
    }

    /**
     * 파생물 크레딧 차감. 같은 베이스로 이미 만든 종류는 AI 가 재사용(derivative_cache)하므로 새로 만들 종류만 센다.
     * 데모(test) 계정은 차감하지 않고 AI 가 미리 만든 결과만 쓴다 (demo=true)
     */
    private Charge chargeBuild(int pNo, String postersJson) {
        List<Map<String, Object>> posters;
        try {
            posters = objectMapper.readValue(postersJson, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "파생물 요청 형식이 올바르지 않습니다.");
        }
        Map<String, Object> made = derivatives(pNo);
        int cost = 0;
        for (Map<String, Object> poster : posters) {
            String posterBase = promotionKey(poster.get("posterImageUrl"));
            String mascotBase = promotionKey(poster.get("mascotImageUrl"));
            Object types = poster.get("types");
            if (!(types instanceof List<?> list)) {
                continue;
            }
            for (Object t : list) {
                String madeBase = promotionKey(made.get(String.valueOf(t)));
                if (madeBase != null && (madeBase.equals(posterBase) || madeBase.equals(mascotBase))) {
                    continue; // 같은 베이스로 이미 만든 결과 재사용
                }
                cost += creditService.derivativeCost(String.valueOf(t));
            }
        }
        return creditService.charge(CurrentMember.no(), CurrentMember.role(), cost, "파생물 생성", pNo);
    }

    /** 경로·URL 에서 "data/promotion/" 이후 부분 (웹 경로와 절대경로를 같은 값으로 비교) */
    private static String promotionKey(Object path) {
        if (path == null) {
            return null;
        }
        String s = path.toString().replace('\\', '/');
        int i = s.indexOf("data/promotion/");
        return i < 0 ? s : s.substring(i + "data/promotion/".length());
    }

    /** 이 프로젝트의 최근 빌드 작업 (진행 화면을 다시 열었을 때 이어서 보기). 없으면 204 */
    @GetMapping("/build-jobs/latest")
    public ResponseEntity<Job> latestBuildJob(@RequestParam("pNo") Integer pNo) {
        projectAccess.check(pNo);
        Job job = jobService.latestFor(pNo, GenerationJobService.EDITOR_BUILD);
        return job == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(job);
    }

    /** 이미 만든 파생물과 그때의 베이스 (같은 베이스면 다시 만들 때 재사용). {유형: 베이스 경로} */
    @SuppressWarnings("unchecked")
    @GetMapping("/project/{pNo}/derivatives")
    public Map<String, Object> derivatives(@PathVariable Integer pNo) {
        projectAccess.check(pNo);
        try {
            Map<String, Object> m = restTemplate.getForObject(pythonApiUrl + "/editor/derivatives/" + pNo, Map.class);
            if (m == null) {
                return Map.of();
            }
            // AI 는 "…/data/promotion/ 이후" 경로를 준다. 웹 경로로 맞춰야 응답에서 화면의 파일 주소(/files/토큰)와 같은 값이 된다
            Map<String, Object> out = new HashMap<>();
            m.forEach((type, base) -> out.put((String) type, "/data/promotion/" + base));
            return out;
        } catch (Exception e) {
            log.warn("파생물 목록 조회 실패 pNo={} ({})", pNo, e.getMessage());
            return Map.of();
        }
    }

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
        boolean demo = CurrentMember.isTest();
        Charge charge = demo ? null : chargeBuild(pNo, postersJson);
        try {
            EditorBuildResponse response = editorBuildService.buildAndSaveTemplates(pNo, postersJson, demo);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            if (charge != null) {
                charge.refund();
            }
            throw e;
        }
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

        // 데모 계정은 DemoGuardInterceptor 가 막는다
        Charge charge = creditService.charge(CurrentMember.no(), CurrentMember.role(),
                creditService.cost("ai-render"), "AI 색상 추천", null);
        try {
            EditorAiRenderResponse res = editorAiRenderService.renderWithAi(request);
            return ResponseEntity.ok(res);
        } catch (RuntimeException e) {
            charge.refund();
            throw e;
        }
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
