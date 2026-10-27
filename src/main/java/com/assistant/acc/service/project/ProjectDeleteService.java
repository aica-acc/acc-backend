package com.assistant.acc.service.project;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.assistant.acc.security.ProjectAccess;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 프로젝트 삭제.
 * information_schema 의 FK 정보를 따라 자식 → 부모 순서로 지우므로 테이블이 추가되어도 따로 고칠 필요가 없다.
 * (기획서, 프롬프트, 생성 기록, 에디터 템플릿, 진행 상태 등 project 에 연결된 모든 행)
 * DB 삭제가 커밋된 뒤에 화면용 생성 파일 폴더(public/data/promotion/{회원}/{프로젝트})도 지운다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectDeleteService {

    private final JdbcTemplate jdbc;
    private final ProjectAccess projectAccess;

    @Value("${react.base-path}")
    private String reactBasePath;

    private record Fk(String childTable, String childCol, String parentCol) {
    }

    /** 로그인한 회원의 프로젝트 하나를 지운다 (관리자는 모든 프로젝트) */
    @Transactional
    public void deleteProject(int pNo) {
        projectAccess.check(pNo);
        String mNo = jdbc.queryForObject("SELECT m_no FROM project WHERE p_no = ?", String.class, pNo);

        deleteAuxiliary(pNo);
        cascadeDelete("project", "p_no = " + pNo, 0);
        log.info("프로젝트 {} (회원 {}) 삭제", pNo, mNo);

        removeFilesAfterCommit(List.of(promotionDir(mNo, pNo)));
    }

    /** 한 회원의 모든 프로젝트를 지운다 (관리자 '회원 데이터 초기화') */
    @Transactional
    public int deleteAllProjectsOf(String mNo) {
        List<Integer> pNos = jdbc.queryForList("SELECT p_no FROM project WHERE m_no = ?", Integer.class, mNo);
        if (pNos.isEmpty()) {
            return 0;
        }
        pNos.forEach(this::deleteAuxiliary);
        cascadeDelete("project", "m_no = '" + mNo.replace("'", "") + "'", 0);
        log.info("회원 {} 의 프로젝트 {}개 삭제", mNo, pNos.size());

        removeFilesAfterCommit(pNos.stream().map(p -> promotionDir(mNo, p)).toList());
        return pNos.size();
    }

    // FK 가 없는 보조 테이블
    private void deleteAuxiliary(int pNo) {
        jdbc.update("DELETE FROM live_poster WHERE p_no = ?", pNo);
        jdbc.update("DELETE FROM poster_archive WHERE project_no = ?", pNo);
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

    private Path promotionDir(String mNo, int pNo) {
        return Paths.get(reactBasePath, "public", "data", "promotion", mNo, String.valueOf(pNo));
    }

    // DB 가 롤백되면 파일은 남겨야 하므로 커밋 이후에 지운다
    private void removeFilesAfterCommit(List<Path> dirs) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                dirs.forEach(ProjectDeleteService::deleteRecursively);
            }
        });
    }

    private static void deleteRecursively(Path dir) {
        if (!Files.isDirectory(dir)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException e) {
                    log.warn("파일 삭제 실패: {} ({})", p, e.getMessage());
                }
            });
        } catch (IOException e) {
            log.warn("폴더 삭제 실패: {} ({})", dir, e.getMessage());
        }
    }
}
