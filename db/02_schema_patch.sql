/* ============================================================
   02. 코드(MyBatis 매퍼 / JdbcTemplate)와 01_schema.sql 의 불일치 보정
   - 모든 매퍼 SQL 을 MySQL PREPARE 로 검증하여 찾아낸 항목만 포함한다.
   - (01_schema.sql 은 `cardnews_ reference_no` 오타를 바로잡았다. 이 오타 때문에 이전에는 그 아래의
      cardnews_score / plan_anlazy / live_poster / editor_template / promotion_path 가 만들어지지 않았다.)
   - 새 DB 초기화 시 01 → 02 → 03 순서로 자동 실행된다.
============================================================ */

/* user_input: 매퍼(UserInputsMapper)는 create_at 을 사용 */
ALTER TABLE user_input CHANGE COLUMN created_at create_at DATE;

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
