/* ============================================================
   V4. 프로젝트 진행 상태 (서버 저장)
   - 기획서 분석 → 유형 선택 → 생성 → 베이스 선택 → 에디터 흐름에서
     화면 사이에 넘기던 값을 브라우저 sessionStorage 대신 서버에 둔다.
   - route : 마지막으로 머문 화면 경로 ("이어서 하기"가 돌아갈 곳)
   - state : 화면 간 공유 값 (키 → JSON 문자열) 을 담은 JSON 객체
   프로젝트가 삭제되면 함께 삭제된다.
============================================================ */

CREATE TABLE IF NOT EXISTS project_progress (
    p_no       INT          NOT NULL PRIMARY KEY,
    m_no       VARCHAR(10)  NOT NULL,
    route      VARCHAR(255) NULL,
    state      MEDIUMTEXT   NULL,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_project_progress_member (m_no, updated_at),
    CONSTRAINT fk_project_progress_project FOREIGN KEY (p_no) REFERENCES project (p_no) ON DELETE CASCADE
);
