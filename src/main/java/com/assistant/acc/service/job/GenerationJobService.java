package com.assistant.acc.service.job;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import com.assistant.acc.security.CurrentMember;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * 백그라운드 생성 작업 (generation_job).
 *
 * 수 분 걸리는 생성 요청을 HTTP 요청과 분리한다. 시작하면 바로 작업 번호를 돌려주고,
 * 화면은 GET /api/jobs/{id} 를 몇 초마다 조회해 진행률을 보여 준다. 사용자는 다른 화면으로 가도 된다.
 * 같은 프로젝트·같은 종류의 작업이 이미 돌고 있으면 새로 만들지 않고 그 작업을 돌려준다 (중복 클릭 = 중복 비용 방지).
 */
@Slf4j
@Service
public class GenerationJobService {

    public static final String EDITOR_BUILD = "EDITOR_BUILD";
    public static final String CREATE_IMAGES = "CREATE_IMAGES";

    // 백엔드가 직접 단계를 진행하는 작업의 진행 상황 (작업 번호 → 진행). 서버 메모리에만 둔다
    private final Map<Long, JobProgress> progresses = new ConcurrentHashMap<>();
    // 단계별 실제 소요 시간 평균 (다음 작업의 예상 시간). 기본값은 첫 작업 전 추정치
    private final Map<String, Double> learnedSeconds = new ConcurrentHashMap<>();

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final TaskExecutor executor;
    private final RestTemplate restTemplate;

    @Value("${python.api.url:http://localhost:5000}")
    private String pythonApiUrl;

    public GenerationJobService(JdbcTemplate jdbc, ObjectMapper objectMapper,
            @Qualifier("jobExecutor") TaskExecutor executor, RestTemplate restTemplate) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
        this.executor = executor;
        this.restTemplate = restTemplate;
    }

    @Data
    public static class Job {
        private Long jobId;
        @JsonProperty("pNo")
        private Integer pNo;
        private String kind;
        private String status;
        private String error;
        private Object result;
        /** 진행 상황 (파생물 생성이면 AI 의 항목별 진행) */
        private Object progress;
        private boolean seen;
        private LocalDateTime createdAt;
        private LocalDateTime startedAt;
        private LocalDateTime finishedAt;
    }

    /** 작업 시작. work 는 별도 스레드에서 실행되므로 요청 정보(CurrentMember 등)를 쓰면 안 된다 */
    public Job start(int pNo, String mNo, String kind, Supplier<Object> work) {
        return start(pNo, mNo, kind, progress -> work.get());
    }

    /** 단계별 진행을 기록하는 작업 (progress.step(...) 으로 단계를 등록하고 progress.run(...) 으로 실행) */
    public Job start(int pNo, String mNo, String kind, Function<JobProgress, Object> work) {
        List<Long> running = jdbc.queryForList(
                "SELECT job_id FROM generation_job WHERE p_no = ? AND kind = ? AND status IN ('QUEUED','RUNNING') "
                        + "ORDER BY job_id DESC LIMIT 1",
                Long.class, pNo, kind);
        if (!running.isEmpty()) {
            log.info("[job] 이미 진행 중인 작업 재사용 pNo={}, kind={}, jobId={}", pNo, kind, running.get(0));
            return load(running.get(0), true);
        }

        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO generation_job (p_no, m_no, kind, status) VALUES (?, ?, ?, 'QUEUED')",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, pNo);
            ps.setString(2, mNo);
            ps.setString(3, kind);
            return ps;
        }, key);
        long jobId = key.getKey().longValue();
        log.info("[job] 시작 jobId={}, pNo={}, kind={}", jobId, pNo, kind);

        JobProgress progress = new JobProgress();
        progresses.put(jobId, progress);
        executor.execute(() -> run(jobId, progress, work));
        return load(jobId, false);
    }

    /** 단계 예상 시간: 이전 작업들의 실제 평균, 없으면 기본값 */
    public double expectSeconds(String step, double defaultSeconds) {
        return learnedSeconds.getOrDefault(step, defaultSeconds);
    }

    private void run(long jobId, JobProgress progress, Function<JobProgress, Object> work) {
        jdbc.update("UPDATE generation_job SET status = 'RUNNING', started_at = NOW() WHERE job_id = ?", jobId);
        try {
            Object result = work.apply(progress);
            progress.finish();
            // 실제 소요 시간을 다음 예상치에 반영 (이동 평균). 기존 결과를 써서 바로 끝난 단계(3초 이하)는 제외
            progress.durations().forEach((step, sec) -> {
                if (sec > 3) {
                    learnedSeconds.merge(step, sec, (old, cur) -> old * 0.6 + cur * 0.4);
                }
            });
            jdbc.update("UPDATE generation_job SET status = 'SUCCEEDED', result = ?, finished_at = NOW() WHERE job_id = ?",
                    objectMapper.writeValueAsString(result), jobId);
            log.info("[job] 완료 jobId={}", jobId);
        } catch (Exception e) {
            // 화면에 보이는 값이라 예외 원문(서버 경로, 내부 주소, 회원번호가 섞임)은 로그에만 남긴다
            jdbc.update("UPDATE generation_job SET status = 'FAILED', error = ?, finished_at = NOW() WHERE job_id = ?",
                    userMessage(e), jobId);
            log.error("[job] 실패 jobId={}", jobId, e);
        }
    }

    /** 사용자에게 보여 줄 실패 사유 */
    private static String userMessage(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof ResponseStatusException rse && rse.getReason() != null) {
                return rse.getReason();
            }
            if (t instanceof org.springframework.web.client.ResourceAccessException) {
                return "AI 서버에 연결하지 못했습니다. 잠시 후 다시 시도해 주세요.";
            }
            if (t instanceof org.springframework.web.client.HttpStatusCodeException) {
                return "AI 서버에서 생성에 실패했습니다. 잠시 후 다시 시도해 주세요.";
            }
        }
        return "생성 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.";
    }

    /** 작업 조회 (본인 작업만) */
    public Job get(long jobId) {
        String owner = jdbc.query("SELECT m_no FROM generation_job WHERE job_id = ?",
                rs -> rs.next() ? rs.getString(1) : null, jobId);
        if (owner == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "작업을 찾을 수 없습니다.");
        }
        if (!owner.equals(CurrentMember.no()) && !CurrentMember.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "다른 회원의 작업입니다.");
        }
        return load(jobId, true);
    }

    /** 헤더 알림용: 진행 중인 작업 + 끝났지만 아직 확인하지 않은 작업(최근 1시간) */
    public List<Job> active() {
        List<Long> ids = jdbc.queryForList("""
                SELECT job_id FROM generation_job
                WHERE m_no = ?
                  AND (status IN ('QUEUED','RUNNING')
                       OR (seen = 0 AND finished_at > NOW() - INTERVAL 1 HOUR))
                ORDER BY job_id DESC LIMIT 10
                """, Long.class, CurrentMember.no());
        return ids.stream().map(id -> load(id, true)).toList();
    }

    /** 프로젝트의 최근 작업 (진행 화면을 다시 열었을 때 이어서 보기) */
    public Job latestFor(int pNo, String kind) {
        List<Long> ids = jdbc.queryForList(
                "SELECT job_id FROM generation_job WHERE p_no = ? AND kind = ? ORDER BY job_id DESC LIMIT 1",
                Long.class, pNo, kind);
        return ids.isEmpty() ? null : load(ids.get(0), true);
    }

    public void markSeen(long jobId) {
        get(jobId);
        jdbc.update("UPDATE generation_job SET seen = 1 WHERE job_id = ?", jobId);
    }

    /** 서버가 재시작되면 돌던 작업은 이어갈 수 없으므로 실패로 표시한다 */
    @EventListener(ApplicationReadyEvent.class)
    public void failInterruptedJobs() {
        int n = jdbc.update("UPDATE generation_job SET status = 'FAILED', error = '서버 재시작으로 중단되었습니다. 다시 시도해 주세요.', "
                + "finished_at = NOW() WHERE status IN ('QUEUED','RUNNING')");
        if (n > 0) {
            log.warn("[job] 서버 재시작으로 중단된 작업 {}개를 실패 처리", n);
        }
    }

    private Job load(long jobId, boolean withProgress) {
        Job job = jdbc.query("SELECT * FROM generation_job WHERE job_id = ?", rs -> rs.next() ? map(rs) : null, jobId);
        if (job != null && withProgress) {
            if (EDITOR_BUILD.equals(job.getKind())) {
                job.setProgress(fetchEditorBuildProgress(job.getPNo()));
            } else if (progresses.containsKey(jobId)) {
                job.setProgress(progresses.get(jobId).snapshot());
            }
        }
        return job;
    }

    @SuppressWarnings("unchecked")
    private Object fetchEditorBuildProgress(int pNo) {
        try {
            return restTemplate.getForObject(pythonApiUrl + "/editor/build/progress/" + pNo, Map.class);
        } catch (Exception e) {
            return null; // 아직 시작 전이거나 AI 서버 재시작으로 기록이 없음
        }
    }

    private Job map(ResultSet rs) throws SQLException {
        Job j = new Job();
        j.setJobId(rs.getLong("job_id"));
        j.setPNo(rs.getInt("p_no"));
        j.setKind(rs.getString("kind"));
        j.setStatus(rs.getString("status"));
        j.setError(rs.getString("error"));
        j.setSeen(rs.getBoolean("seen"));
        j.setCreatedAt(toLocal(rs.getTimestamp("created_at")));
        j.setStartedAt(toLocal(rs.getTimestamp("started_at")));
        j.setFinishedAt(toLocal(rs.getTimestamp("finished_at")));
        String result = rs.getString("result");
        if (result != null) {
            try {
                j.setResult(objectMapper.readValue(result, Object.class));
            } catch (Exception e) {
                j.setResult(result);
            }
        }
        return j;
    }

    private static LocalDateTime toLocal(Timestamp t) {
        return t == null ? null : t.toLocalDateTime();
    }
}
