package com.assistant.acc.service.progress;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.assistant.acc.domain.project.ProposalMetadata;
import com.assistant.acc.mapper.project.ProjectMapper;
import com.assistant.acc.security.CurrentMember;
import com.assistant.acc.security.ProjectAccess;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * 프로젝트 진행 상태 (project_progress).
 *
 * 화면 사이에 넘기던 값(state)과 마지막 화면 경로(route)를 서버에 저장해,
 * 새로고침·브라우저 종료·다른 기기에서도 같은 단계부터 이어서 할 수 있게 한다.
 * state 는 "키 → 값(JSON 문자열)" 맵으로, 프론트의 sessionStorage 와 같은 모양이다.
 */
@Service
@RequiredArgsConstructor
public class ProjectProgressService {

    /** 진행 기록이 없는 프로젝트를 열 때 돌아갈 화면 (분석 결과) */
    public static final String DEFAULT_ROUTE = "/analyze/list";

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final ProjectMapper projectMapper;
    private final ProjectAccess projectAccess;

    @Data
    public static class Progress {
        @JsonProperty("pNo") // Lombok getter(getPNo)는 기본 직렬화 시 "pno" 가 된다
        private Integer pNo;
        private String title;
        private String route;
        private Map<String, String> state;
        private LocalDateTime updatedAt;
    }

    @Data
    public static class ProgressUpdate {
        /** 추가·변경할 키 (값은 JSON 문자열) */
        private Map<String, String> set;
        /** 삭제할 키 */
        private List<String> remove;
        /** 마지막 화면 경로. null 이면 그대로 둔다 */
        private String route;
    }

    /** 로그인한 회원이 가장 최근에 진행한 프로젝트. 없으면 null */
    public Progress current() {
        String mNo = CurrentMember.no();
        Integer pNo = jdbcTemplate.query(
                "SELECT p_no FROM project_progress WHERE m_no = ? ORDER BY updated_at DESC, p_no DESC LIMIT 1",
                rs -> rs.next() ? rs.getInt(1) : null,
                mNo);
        if (pNo == null) {
            // 진행 기록이 아직 없으면(이전 버전에서 만든 프로젝트) 가장 최근 프로젝트
            pNo = projectMapper.selectLatestProjectNo(mNo);
        }
        return pNo == null ? null : load(pNo);
    }

    /** 특정 프로젝트의 진행 상태. 기록이 없으면 DB 의 기획서 정보로 시작 상태를 만든다 */
    public Progress get(Integer pNo) {
        projectAccess.check(pNo);
        return load(pNo);
    }

    @Transactional
    public Progress update(Integer pNo, ProgressUpdate update) {
        projectAccess.check(pNo);
        Progress progress = load(pNo);
        Map<String, String> state = progress.getState();
        if (update.getSet() != null) {
            state.putAll(update.getSet());
        }
        if (update.getRemove() != null) {
            update.getRemove().forEach(state::remove);
        }
        String route = update.getRoute() != null ? update.getRoute() : progress.getRoute();

        jdbcTemplate.update(
                "INSERT INTO project_progress (p_no, m_no, route, state) VALUES (?, (SELECT m_no FROM project WHERE p_no = ?), ?, ?) "
                        + "ON DUPLICATE KEY UPDATE route = VALUES(route), state = VALUES(state)",
                pNo, pNo, route, toJson(state));
        return load(pNo);
    }

    private Progress load(Integer pNo) {
        Progress progress = jdbcTemplate.query(
                "SELECT route, state, updated_at FROM project_progress WHERE p_no = ?",
                rs -> {
                    if (!rs.next()) {
                        return null;
                    }
                    Progress p = new Progress();
                    p.setRoute(rs.getString("route"));
                    p.setState(fromJson(rs.getString("state")));
                    Timestamp updatedAt = rs.getTimestamp("updated_at");
                    p.setUpdatedAt(updatedAt == null ? null : updatedAt.toLocalDateTime());
                    return p;
                },
                pNo);

        ProposalMetadata metadata = projectMapper.selectProposalMetadata(pNo);
        if (progress == null) {
            progress = new Progress();
            progress.setRoute(DEFAULT_ROUTE);
            progress.setState(new LinkedHashMap<>());
        }
        // 기획서 정보는 DB 가 원본이다. 화면에서 수정해 저장한 값이 없을 때만 채운다
        if (metadata != null && !progress.getState().containsKey("proposalData")) {
            progress.getState().put("proposalData", toJson(metadata));
        }
        progress.setPNo(pNo);
        progress.setTitle(metadata == null ? null : metadata.getTitle());
        return progress;
    }

    private Map<String, String> fromJson(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, String>>() {
            });
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("project_progress.state 를 읽을 수 없습니다.", e);
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
