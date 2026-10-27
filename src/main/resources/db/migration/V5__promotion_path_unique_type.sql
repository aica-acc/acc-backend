/* ============================================================
   V5. promotion_path: 프로젝트마다 홍보물 유형별 한 행
   - 파생 만들기를 다시 시도할 때마다 베이스 포스터/마스코트가 새 행으로 쌓였다.
   - 기존 중복은 유형별 최신 행만 남기고 지운 뒤 (p_no, db_file_type) 유니크 제약을 건다.
   - 저장은 PromotionPathMapper.insertPromotionPath 의 ON DUPLICATE KEY UPDATE 로 교체된다.
============================================================ */

DELETE older
FROM promotion_path older
JOIN promotion_path newer
  ON older.p_no = newer.p_no
 AND older.db_file_type = newer.db_file_type
 AND older.promotion_path_no < newer.promotion_path_no;

ALTER TABLE promotion_path
    ADD UNIQUE KEY uk_promotion_path_project_type (p_no, db_file_type);
