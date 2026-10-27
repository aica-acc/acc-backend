package com.assistant.acc.service.editor;

import org.springframework.beans.factory.annotation.Value;
import com.assistant.acc.dto.editor.EditorBuildResponse;
import com.assistant.acc.dto.editor.PythonBuildResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

@Slf4j
@Service
@RequiredArgsConstructor
public class EditorBuildServiceImpl implements EditorBuildService {

    @Value("${python.api.url:http://localhost:5000}")
    private String pythonApiUrl;

    @Value("${react.base-path}")
    private String reactBasePath;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final EditorTemplateService editorTemplateService;
    private final PromotionPathService promotionPathService;

    /** "/data/promotion/..." 웹 경로 → 프론트 public 폴더의 절대경로 (이미 절대경로인 값은 그대로) */
    private String toAbsolutePaths(String json) {
        String publicDir = Paths.get(reactBasePath, "public").toAbsolutePath().normalize().toString().replace('\\', '/');
        return json.replaceAll("(?<=\")/data/promotion/", Matcher.quoteReplacement(publicDir + "/data/promotion/"));
    }

    @Override
    public EditorBuildResponse buildAndSaveTemplates(int pNo, String postersJson) {
        try {
            // 1) postersJson 파싱
            //    화면은 이제 웹 경로(/files 토큰 → /data/promotion/...)만 보낸다. AI 생성기는 예전처럼 절대경로를 받으므로 여기서 바꾼다
            List<Map<String, Object>> posters =
                    objectMapper.readValue(toAbsolutePaths(postersJson), new TypeReference<List<Map<String, Object>>>() {});

            // 2) Python payload 구성
            Map<String, Object> payload = new HashMap<>();
            payload.put("pNo", pNo);
            payload.put("posters", posters);

            log.info("🚀 [EditorBuildService] call Python editor.build, pNo={}, posters_count={}",
                    pNo, posters.size());

            // 3) Python 호출
            PythonBuildResponse pyResp = restTemplate.postForObject(
                    pythonApiUrl + "/editor/build",
                    payload,
                    PythonBuildResponse.class
            );

            if (pyResp == null) {
                throw new IllegalStateException("Python editor.build response is null");
            }

            log.info("✅ [EditorBuildService] Python response status={}, pNo={}, filePath={}",
                    pyResp.getStatus(), pyResp.getPNo(), pyResp.getFilePath());

            // 4) total.json 경로 저장 (editor_template)
            String filePath = pyResp.getFilePath();
            if (filePath == null || filePath.isBlank()) {
                throw new IllegalStateException("Python editor.build returned empty filePath");
            }

            log.info("💾 [EditorBuildService] save template pNo={}, filePath={}", pNo, filePath);
            editorTemplateService.saveEditorTemplate(pNo, filePath);

            // 5) 🔥 여기서부터 promotion_path 저장 로직
            List<String> dbFilePaths = pyResp.getDbFilePath();
            List<String> dbFileTypes = pyResp.getDbFileType();

            log.info("💾 [EditorBuildService] Python dbFilePath={}, dbFileType={}",
                    dbFilePaths, dbFileTypes);

            log.info("💾 [EditorBuildService] save promotion paths, count={}",
                    (dbFilePaths != null ? dbFilePaths.size() : 0));

            // 실제 DB insert
            promotionPathService.savePromotionPaths(pNo, dbFilePaths, dbFileTypes);

            // 6) 프론트 응답
            EditorBuildResponse resp = new EditorBuildResponse();
            resp.setPNo(pNo);
            resp.setStatus(pyResp.getStatus() != null ? pyResp.getStatus() : "success");

            return resp;

        } catch (Exception e) {
            log.error("❌ [EditorBuildService] buildAndSaveTemplates error", e);
            throw new RuntimeException("Failed to build and save templates", e);
        }
    }
}
