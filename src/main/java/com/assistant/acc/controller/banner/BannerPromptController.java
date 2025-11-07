package com.assistant.acc.controller.banner;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/banner")
public class BannerPromptController {

    private static final Pattern DONE_LINE = Pattern.compile("^\\[Done\\]\\s+(.+)$");

    // 헬스체크
    @GetMapping("")
    public String hello() { return "HelloBanner"; }

    // (기존 에코: 원하면 남겨둬도 됨)
    @PostMapping("/prompt")
    public ResponseEntity<?> echo(@RequestBody(required = false) Map<String, Object> body) {
        return ResponseEntity.ok(Map.of("ok", true, "received", body));
    }

    /**
     * 본문(JSON) 그대로 받아서 임시 analysis.json 저장 → 파이썬 실행 → 결과 JSON 읽어서 반환
     * 예) POST /banner/prompt-body?script=C:\...\horizontal_3024_544_make_prompt_from_analysis.py&python=python&width=3024&height=544&aspect=custom&res=2K&usePreLlm=true
     */
    @PostMapping(value = "/prompt-body", consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> buildPromptFromBody(
            @RequestParam(name = "script") String scriptPath,
            @RequestParam(name = "python", required = false, defaultValue = "python") String pythonExe,
            @RequestParam(name = "width", required = false) Integer width,
            @RequestParam(name = "height", required = false) Integer height,
            @RequestParam(name = "aspect", required = false) String aspectRatio,
            @RequestParam(name = "res", required = false) String resolution,
            @RequestParam(name = "usePreLlm", required = false) Boolean usePreLlm,
            @RequestParam(name = "seed", required = false) Integer seed,
            @RequestBody String rawJsonBody
    ) {
        try {
            if (rawJsonBody == null || rawJsonBody.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "Empty body"));
            }

            // 1) 임시 analysis.json 저장
            Path tmp = Files.createTempFile("analysis_", ".json");
            Files.writeString(tmp, rawJsonBody, StandardCharsets.UTF_8);

            // 2) 파이썬 프로세스 실행 (input() 1~7 순서대로 답 주입)
            List<String> cmd = new ArrayList<>();
            cmd.add(pythonExe);
            cmd.add(scriptPath);

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(new File("."));                 // out/가 백엔드 폴더 아래 생성됨
            pb.redirectErrorStream(true);
            Map<String, String> env = pb.environment();
            env.putIfAbsent("PYTHONIOENCODING", "utf-8");

            Process ps = pb.start();
            try (BufferedWriter w = new BufferedWriter(new OutputStreamWriter(ps.getOutputStream(), StandardCharsets.UTF_8))) {
                // 1) analysis.json 경로
                w.write(tmp.toAbsolutePath().toString()); w.newLine();
                // 2) width
                w.write(width != null ? String.valueOf(width) : ""); w.newLine();
                // 3) height
                w.write(height != null ? String.valueOf(height) : ""); w.newLine();
                // 4) aspect_ratio
                w.write(aspectRatio != null ? aspectRatio : ""); w.newLine();
                // 5) resolution
                w.write(resolution != null ? resolution : ""); w.newLine();
                // 6) use_pre_llm
                w.write(usePreLlm != null ? String.valueOf(usePreLlm) : ""); w.newLine();
                // 7) seed
                w.write(seed != null ? String.valueOf(seed) : ""); w.newLine();
                w.flush();
            }

            StringBuilder log = new StringBuilder();
            String outPathStr = null;
            try (BufferedReader r = new BufferedReader(new InputStreamReader(ps.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    log.append(line).append("\n");
                    Matcher m = DONE_LINE.matcher(line.trim());
                    if (m.find()) outPathStr = m.group(1).trim();
                }
            }

            boolean finished = ps.waitFor(Duration.ofMinutes(3).toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
            if (!finished) {
                ps.destroyForcibly();
                return ResponseEntity.internalServerError().body(Map.of("ok", false, "error", "Python timeout", "log", log.toString()));
            }
            int exit = ps.exitValue();
            if (exit != 0) {
                return ResponseEntity.internalServerError().body(Map.of("ok", false, "error", "Python exit " + exit, "log", log.toString()));
            }

            Map<String, Object> res = new LinkedHashMap<>();
            res.put("ok", true);
            res.put("log", log.toString());
            res.put("outPath", outPathStr);

            if (outPathStr != null && Files.exists(Path.of(outPathStr))) {
                String json = Files.readString(Path.of(outPathStr), StandardCharsets.UTF_8);
                res.put("promptJsonRaw", json);
                try {
                    com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                    @SuppressWarnings("unchecked")
                    Map<String,Object> parsed = om.readValue(json, LinkedHashMap.class);
                    res.put("promptJson", parsed);
                } catch (Exception ignore) {}
            }

            try { Files.deleteIfExists(tmp); } catch (Exception ignore) {}
            return ResponseEntity.ok(res);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("ok", false, "error", e.getMessage()));
        }
    }
}
