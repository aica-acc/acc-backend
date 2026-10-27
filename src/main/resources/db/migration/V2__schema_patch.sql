/* ============================================================
   V2. 코드(MyBatis 매퍼 / JdbcTemplate)와 V1 스키마의 불일치 보정
   - 모든 매퍼 SQL 을 MySQL PREPARE 로 검증해 찾아낸 항목
   - 이미 보정된 DB 에서 다시 실행해도 오류가 나지 않도록 조건부로 실행한다
============================================================ */

/* user_input: 매퍼(UserInputsMapper)는 create_at 을 사용 (원본 DDL 은 created_at) */
SET @sql = IF(
    EXISTS(SELECT 1 FROM information_schema.columns
           WHERE table_schema = DATABASE() AND table_name = 'user_input' AND column_name = 'created_at'),
    'ALTER TABLE user_input CHANGE COLUMN created_at create_at DATE',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

/* poster_archive: 포스터 아카이브 (PosterArchiveMapper / ImageMapper) */
CREATE TABLE IF NOT EXISTS poster_archive (
    file_path_no INT AUTO_INCREMENT PRIMARY KEY,
    project_no INT,
    file_name VARCHAR(300),
    file_url VARCHAR(500),
    visual_prompt TEXT,
    style_name VARCHAR(250),
    create_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
