package com.assistant.acc.service.admin;

import java.util.ArrayList;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 한 회원의 프로젝트와 그 하위 DB 기록(FK 로 연결된 모든 테이블)을 삭제한다.
 * information_schema 의 FK 정보를 따라 자식 → 부모 순서로 지우므로 테이블이 추가되어도 따로 고칠 필요가 없다.
 * (디스크에 생성된 이미지/영상 파일은 지우지 않는다.)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TestDataResetService {

    private final JdbcTemplate jdbc;

    private record Fk(String childTable, String childCol, String parentCol) {
    }

    @Transactional
    public int deleteAllProjectsOf(String mNo) {
        List<Integer> pNos = jdbc.queryForList("SELECT p_no FROM project WHERE m_no = ?", Integer.class, mNo);
        if (pNos.isEmpty()) {
            return 0;
        }
        // FK 가 없는 보조 테이블
        for (Integer p : pNos) {
            jdbc.update("DELETE FROM live_poster WHERE p_no = ?", p);
            jdbc.update("DELETE FROM poster_archive WHERE project_no = ?", p);
        }
        cascadeDelete("project", "m_no = '" + mNo.replace("'", "") + "'", 0);
        log.info("회원 {} 의 프로젝트 {}개 삭제", mNo, pNos.size());
        return pNos.size();
    }

    private void cascadeDelete(String table, String where, int depth) {
        if (depth > 8) {
            throw new IllegalStateException("FK 깊이가 너무 깊습니다: " + table);
        }
        for (Fk fk : childrenOf(table)) {
            String sub = fk.childCol() + " IN (SELECT " + fk.parentCol() + " FROM " + table + " WHERE " + where + ")";
            cascadeDelete(fk.childTable(), sub, depth + 1);
        }
        jdbc.update("DELETE FROM " + table + " WHERE " + where);
    }

    private List<Fk> childrenOf(String parentTable) {
        List<Fk> out = new ArrayList<>();
        jdbc.query("""
                SELECT table_name, column_name, referenced_column_name
                FROM information_schema.key_column_usage
                WHERE table_schema = DATABASE() AND referenced_table_name = ?
                """, rs -> {
            out.add(new Fk(rs.getString(1), rs.getString(2), rs.getString(3)));
        }, parentTable);
        return out;
    }
}
