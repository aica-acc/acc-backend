package com.assistant.acc.service.banner;

import com.assistant.acc.dto.banner.BannerPromptRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class BannerPromptService {

    private static final Pattern DONE_LINE = Pattern.compile("^\\[Done\\]\\s+(.+)$");

    public Map<String, Object> runPromptBuilder(BannerPromptRequest req) throws Exception {
        // 필수값 검증 (하드코딩 방지: 없으면 400 성격의 에러로 반환)
        if (isBlank(req.getPythonExe()))  throw new IllegalArgumentException("pythonExe is required");
        if (isBlank(req.getScriptPath())) throw new IllegalArgumentException("scriptPath is required");
        if (isBlank(req.getAnalysisPath())) throw new IllegalArgumentException("analysisPath is required");

        List<String> cmd = new ArrayList<>();
        cmd.add(req.getPythonExe());
        cmd.add(req.getScriptPath());

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(new File("."));                 // out/가 백엔드 폴더 아래 생김
        pb.redirectErrorStream(true);                // stderr -> stdout 합치기
        Map<String, String> env = pb.environment();
        env.putIfAbsent("PYTHONIOENCODING", "utf-8"); // 윈도우 인코딩 이슈 회피

        Process ps = pb.start();

        // 파이썬 input() 질문에 순서대로 답 넣기
        try (BufferedWriter w = new BufferedWriter(new OutputStreamWriter(ps.getOutputStream(), StandardCharsets.UTF_8))) {
            // 1) analysis.json 경로
            w.write(req.getAnalysisPath()); w.newLine();

            // 2) width
            w.write(req.getWidth() != null ? String.valueOf(req.getWidth()) : ""); w.newLine();

            // 3) height
            w.write(req.getHeight() != null ? String.valueOf(req.getHeight()) : ""); w.newLine();

            // 4) aspect_ratio
            w.write(req.getAspectRatio() != null ? req.getAspectRatio() : ""); w.newLine();

            // 5) resolution
            w.write(req.getResolution() != null ? req.getResolution() : ""); w.newLine();

            // 6) use_pre_llm(true/false)
            w.write(req.getUsePreLlm() != null ? String.valueOf(req.getUsePreLlm()) : ""); w.newLine();

            // 7) seed (엔터로 스킵)
            w.write(req.getSeed() != null ? String.valueOf(req.getSeed()) : ""); w.newLine();

            w.flush();
        }

        // 로그 수집 + [Done] 라인에서 출력 JSON 경로 추출
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
            throw new RuntimeException("Python process timeout (3 min). Log:\n" + log);
        }
        int exit = ps.exitValue();
        if (exit != 0) {
            throw new RuntimeException("Python exited with code " + exit + ". Log:\n" + log);
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("log", log.toString());
        res.put("outPath", outPathStr);

        // 결과 JSON 읽어 반환(가능한 경우)
        if (outPathStr != null && Files.exists(Path.of(outPathStr))) {
            String json = Files.readString(Path.of(outPathStr), StandardCharsets.UTF_8);
            res.put("promptJsonRaw", json);
            try {
                ObjectMapper om = new ObjectMapper();
                @SuppressWarnings("unchecked")
                Map<String,Object> parsed = om.readValue(json, LinkedHashMap.class);
                res.put("promptJson", parsed);
            } catch (Exception ignore) {
                // 파싱 실패해도 raw 그대로 제공
            }
        }
        return res;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
