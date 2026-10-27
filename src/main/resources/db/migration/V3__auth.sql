/* ============================================================
   V3. 인증(JWT) 지원
   - password_hash : BCrypt 해시 (소셜/레거시 계정은 NULL 허용)
   - role          : USER(일반) / TEST(테스트 계정) / ADMIN(관리자)
   계정 생성(test, admin)은 백엔드 기동 시 DataInitializer 가 처리한다.
   이미 컬럼이 있는 DB 에서도 오류 없이 넘어가도록 조건부로 실행한다.
============================================================ */

SET @sql = IF(
    EXISTS(SELECT 1 FROM information_schema.columns
           WHERE table_schema = DATABASE() AND table_name = 'member' AND column_name = 'password_hash'),
    'SELECT 1',
    'ALTER TABLE member ADD COLUMN password_hash VARCHAR(100) NULL AFTER m_phone');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(SELECT 1 FROM information_schema.columns
           WHERE table_schema = DATABASE() AND table_name = 'member' AND column_name = 'role'),
    'SELECT 1',
    'ALTER TABLE member ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT ''USER'' AFTER password_hash');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE member MODIFY COLUMN state VARCHAR(50) DEFAULT 'ACTIVE';
