package com.assistant.acc.service.job;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 백엔드가 직접 단계를 진행하는 작업의 진행 상황 (예: 포스터·마스코트 생성).
 * AI 의 파생물 빌드 진행(/editor/build/progress)과 같은 모양으로 내보내, 화면은 같은 진행 UI 를 쓴다.
 *   { status, phase, items: [{type, label, status, expectSeconds, startedAt, finishedAt}], total, done, now }
 * 시간은 초 단위 epoch (AI 쪽과 같음).
 */
public class JobProgress {

    private final List<Map<String, Object>> items = new ArrayList<>();
    private String status = "running";
    private String phase = "준비 중";

    /** 단계 등록. expectSeconds: 예상 소요 시간 (진행률·남은 시간 계산용) */
    public synchronized JobProgress step(String type, String label, double expectSeconds) {
        Map<String, Object> it = new LinkedHashMap<>();
        it.put("type", type);
        it.put("label", label);
        it.put("status", "pending");
        it.put("reused", false);
        it.put("expectSeconds", Math.round(expectSeconds));
        it.put("startedAt", null);
        it.put("finishedAt", null);
        items.add(it);
        return this;
    }

    /** 단계를 실행하며 상태를 기록한다. 실패하면 예외를 그대로 던진다 */
    public <T> T run(String type, Supplier<T> work) {
        Map<String, Object> it = find(type);
        synchronized (this) {
            phase = (String) it.get("label");
            it.put("status", "running");
            it.put("startedAt", now());
        }
        try {
            T result = work.get();
            synchronized (this) {
                it.put("status", "done");
                it.put("finishedAt", now());
            }
            return result;
        } catch (RuntimeException e) {
            synchronized (this) {
                it.put("status", "failed");
                it.put("finishedAt", now());
                status = "failed";
            }
            throw e;
        }
    }

    public synchronized void finish() {
        status = "done";
        phase = "완료";
    }

    /** 끝난 단계의 실제 소요 시간 (다음 작업의 예상 시간 학습용) */
    public synchronized Map<String, Double> durations() {
        Map<String, Double> out = new LinkedHashMap<>();
        for (Map<String, Object> it : items) {
            if ("done".equals(it.get("status")) && it.get("startedAt") != null) {
                out.put((String) it.get("type"), (Double) it.get("finishedAt") - (Double) it.get("startedAt"));
            }
        }
        return out;
    }

    public synchronized Map<String, Object> snapshot() {
        List<Map<String, Object>> copy = new ArrayList<>();
        int done = 0;
        for (Map<String, Object> it : items) {
            copy.add(new LinkedHashMap<>(it));
            if ("done".equals(it.get("status"))) {
                done++;
            }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("status", status);
        out.put("phase", phase);
        out.put("items", copy);
        out.put("total", copy.size());
        out.put("done", done);
        out.put("now", now());
        return out;
    }

    private Map<String, Object> find(String type) {
        synchronized (this) {
            return items.stream().filter(i -> type.equals(i.get("type"))).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("등록되지 않은 단계: " + type));
        }
    }

    private static double now() {
        return Math.round(System.currentTimeMillis() / 100.0) / 10.0;
    }
}
