/* ============================================================
   V7. 크레딧과 리포트 저장
   - member.credit  : 남은 크레딧. 유료 AI 생성(포스터·파생물·영상·분석 등)을 시작할 때 차감한다
   - credit_ledger  : 지급·차감·환불 기록 (잔액 추적과 문의 대응용)
   - report_cache   : 리포트(보도자료·공고·SNS·패키지)를 프로젝트별로 저장. 예전에는 화면을 열 때마다 AI 를 다시 불렀다
   - 이미 가입한 일반 회원(USER)에게도 가입 기념 크레딧(500)을 한 번 지급한다
============================================================ */

SET @sql = IF(
    EXISTS(SELECT 1 FROM information_schema.columns
           WHERE table_schema = DATABASE() AND table_name = 'member' AND column_name = 'credit'),
    'SELECT 1',
    'ALTER TABLE member ADD COLUMN credit INT NOT NULL DEFAULT 0 AFTER role');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS credit_ledger (
    ledger_no  BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    m_no       VARCHAR(10)  NOT NULL,
    delta      INT          NOT NULL,
    balance    INT          NOT NULL,
    reason     VARCHAR(40)  NOT NULL,
    p_no       INT          NULL,
    memo       VARCHAR(200) NULL,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_credit_ledger_member (m_no, created_at)
);

CREATE TABLE IF NOT EXISTS report_cache (
    p_no        INT          NOT NULL,
    report_type VARCHAR(20)  NOT NULL,
    content     MEDIUMTEXT   NOT NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (p_no, report_type),
    CONSTRAINT fk_report_cache_project FOREIGN KEY (p_no) REFERENCES project (p_no) ON DELETE CASCADE
);

UPDATE member m
SET m.credit = m.credit + 500
WHERE m.role = 'USER'
  AND NOT EXISTS (SELECT 1 FROM credit_ledger l WHERE l.m_no = m.m_no AND l.reason = 'SIGNUP_BONUS');

INSERT INTO credit_ledger (m_no, delta, balance, reason, memo)
SELECT m.m_no, 500, m.credit, 'SIGNUP_BONUS', '가입 기념 (기존 회원 일괄 지급)'
FROM member m
WHERE m.role = 'USER'
  AND NOT EXISTS (SELECT 1 FROM credit_ledger l WHERE l.m_no = m.m_no AND l.reason = 'SIGNUP_BONUS');
