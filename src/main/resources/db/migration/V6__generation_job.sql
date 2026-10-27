/* ============================================================
   V6. 생성 작업 (백그라운드)
   - 파생물 생성(에디터 빌드)처럼 수 분 걸리는 작업을 요청과 분리해 서버에서 실행한다.
   - 요청은 바로 작업 번호를 돌려주고, 화면은 이 테이블과 AI 진행 상황으로 진행률을 보여 준다.
   - status: QUEUED → RUNNING → SUCCEEDED | FAILED
   - seen  : 사용자가 완료 알림을 확인했는지 (헤더 알림 표시용)
============================================================ */

CREATE TABLE IF NOT EXISTS generation_job (
    job_id      BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    p_no        INT          NOT NULL,
    m_no        VARCHAR(10)  NOT NULL,
    kind        VARCHAR(30)  NOT NULL,
    status      VARCHAR(20)  NOT NULL,
    result      MEDIUMTEXT   NULL,
    error       TEXT         NULL,
    seen        TINYINT(1)   NOT NULL DEFAULT 0,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at  DATETIME     NULL,
    finished_at DATETIME     NULL,
    KEY idx_generation_job_member (m_no, created_at),
    KEY idx_generation_job_project (p_no, kind, status),
    CONSTRAINT fk_generation_job_project FOREIGN KEY (p_no) REFERENCES project (p_no) ON DELETE CASCADE
);
