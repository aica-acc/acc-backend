/* ============================================================
   V8. 소셜 로그인 (카카오 / 구글)
   - social_provider : KAKAO / GOOGLE (일반 가입은 NULL)
   - social_id       : 제공자의 회원 식별자
   소셜로 처음 로그인하면 일반 회원(USER)으로 가입되고 가입 기념 크레딧을 받는다.
============================================================ */

SET @sql = IF(
    EXISTS(SELECT 1 FROM information_schema.columns
           WHERE table_schema = DATABASE() AND table_name = 'member' AND column_name = 'social_provider'),
    'SELECT 1',
    'ALTER TABLE member ADD COLUMN social_provider VARCHAR(20) NULL AFTER password_hash, ADD COLUMN social_id VARCHAR(100) NULL AFTER social_provider');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(SELECT 1 FROM information_schema.statistics
           WHERE table_schema = DATABASE() AND table_name = 'member' AND index_name = 'uk_member_social'),
    'SELECT 1',
    'ALTER TABLE member ADD UNIQUE KEY uk_member_social (social_provider, social_id)');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
