/* ============================================================
   03. 인증(JWT) 지원
   - password_hash : BCrypt 해시 (소셜/레거시 계정은 NULL 허용)
   - role          : USER(일반) / TEST(테스트 계정) / ADMIN(관리자)
   계정 생성(test, admin)은 백엔드 기동 시 DataInitializer 가 처리한다.
============================================================ */
ALTER TABLE member
    ADD COLUMN password_hash VARCHAR(100) NULL AFTER m_phone,
    ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER' AFTER password_hash;
ALTER TABLE member MODIFY COLUMN state VARCHAR(50) DEFAULT 'ACTIVE';
