/* ============================================================
   ACC PROJECT – FULL DATABASE DDL
   테이블 생성 순서: FK 의존성 높은 테이블이 뒤로 가도록 정렬됨
   모든 테이블 = 네가 준 설계 100% 그대로 반영
============================================================ */

/* ============================================================
   1. member (회원)
============================================================ */
CREATE TABLE member (
    m_no VARCHAR(10) PRIMARY KEY,
    m_email VARCHAR(50) UNIQUE NOT NULL,
    m_name VARCHAR(50) NOT NULL,
    m_phone VARCHAR(50) NOT NULL,
    create_at DATE NOT NULL,
    delete_at DATE,
    state VARCHAR(50)
);

/* ============================================================
   2. project (프로젝트) - member FK
============================================================ */
CREATE TABLE project (
    p_no INT AUTO_INCREMENT PRIMARY KEY,
    m_no VARCHAR(10) NOT NULL,
    create_at DATE DEFAULT (CURRENT_DATE),
    FOREIGN KEY (m_no) REFERENCES member(m_no)
);

/* ============================================================
   3. user_input (유저 입력값) - project FK
============================================================ */
CREATE TABLE user_input (
    user_input_no INT AUTO_INCREMENT PRIMARY KEY,
    p_no INT NOT NULL,
    theme TEXT NOT NULL,
    keywords JSON NOT NULL,
    p_name VARCHAR(250) NOT NULL,
    created_at DATE,
    FOREIGN KEY (p_no) REFERENCES project(p_no)
);

/* ============================================================
   4. proposal_file_path (기획서 파일 경로) - user_input FK
============================================================ */
CREATE TABLE proposal_file_path (
    proposal_file_path_no INT AUTO_INCREMENT PRIMARY KEY,
    user_input_no INT NOT NULL,
    proposal_file_path VARCHAR(250) NOT NULL,
    create_at DATE NOT NULL,
    proposal_file_name VARCHAR(250) NOT NULL,
    FOREIGN KEY (user_input_no) REFERENCES user_input(user_input_no)
);

/* ============================================================
   5. file_path (프로젝트 파일 경로) - project FK
============================================================ */
CREATE TABLE file_path (
    file_path_no INT AUTO_INCREMENT PRIMARY KEY,
    p_no INT NOT NULL,
    db_file_path VARCHAR(250) NOT NULL,
    db_file_type VARCHAR(250) NOT NULL,
    create_at DATE NOT NULL,
    FOREIGN KEY (p_no) REFERENCES project(p_no)
);

/* ============================================================
   6. prompt (프롬프트) - user_input FK
============================================================ */
CREATE TABLE prompt (
    prompt_no INT AUTO_INCREMENT PRIMARY KEY,
    user_input_no INT NOT NULL,
    visual_prompt TEXT NOT NULL,
    style_name VARCHAR(250) NOT NULL,
    promotion_type VARCHAR(50) NOT NULL,
    created_at DATE NOT NULL,
    FOREIGN KEY (user_input_no) REFERENCES user_input(user_input_no)
);

/* ============================================================
   7. promotion (홍보물) - project FK, prompt FK
============================================================ */
CREATE TABLE promotion (
    promotion_no INT AUTO_INCREMENT PRIMARY KEY,
    p_no INT NOT NULL,
    prompt_no INT NOT NULL,
    promotion_type VARCHAR(250),
    created_at DATE NOT NULL,
    updated_at DATE,
    FOREIGN KEY (p_no) REFERENCES project(p_no),
    FOREIGN KEY (prompt_no) REFERENCES prompt(prompt_no)
);

/* ============================================================
   8. generated_asset (생성된 파생물) - promotion, prompt FK
============================================================ */
CREATE TABLE generated_asset (
    generated_asset_no INT AUTO_INCREMENT PRIMARY KEY,
    promotion_no INT NOT NULL,
    prompt_no INT NOT NULL,
    is_main TINYINT NOT NULL,
    generate_asset_type VARCHAR(50) NOT NULL,
    created_at DATE NOT NULL,
    updated_at DATE,
    FOREIGN KEY (promotion_no) REFERENCES promotion(promotion_no),
    FOREIGN KEY (prompt_no) REFERENCES prompt(prompt_no)
);

/* ============================================================
   9. generate_file_path (파생물 파일 경로) - generated_asset FK
============================================================ */
CREATE TABLE generate_file_path (
    generate_file_path_no INT AUTO_INCREMENT PRIMARY KEY,
    generated_asset_no INT NOT NULL,
    file_path TEXT NOT NULL,
    file_name VARCHAR(300) NOT NULL,
    extension VARCHAR(300) NOT NULL,
    created_at DATE NOT NULL,
    FOREIGN KEY (generated_asset_no) REFERENCES generated_asset(generated_asset_no)
);

/* ============================================================
   10. video (영상) - promotion FK, generate_file_path FK
============================================================ */
CREATE TABLE video (
    video_no INT AUTO_INCREMENT PRIMARY KEY,
    promotion_no INT NOT NULL,
    generate_file_path_no INT NOT NULL,
    create_at DATE NOT NULL,
    category VARCHAR(512) NOT NULL,
    FOREIGN KEY (promotion_no) REFERENCES promotion(promotion_no),
    FOREIGN KEY (generate_file_path_no) REFERENCES generate_file_path(generate_file_path_no)
);

/* ============================================================
   11. proposal_metadata (기획서 메타데이터) - project FK
============================================================ */
CREATE TABLE proposal_metadata (
    proposal_metadata_no INT AUTO_INCREMENT PRIMARY KEY,
    p_no INT NOT NULL,
    title TEXT NOT NULL,
    festival_start_date DATE NOT NULL,
    festival_end_date DATE NOT NULL,
    location VARCHAR(512) NOT NULL,
    host VARCHAR(512) NOT NULL,
    organizer VARCHAR(512) NOT NULL,
    target VARCHAR(512) NOT NULL,
    contactInfo VARCHAR(512) NOT NULL,
    directions VARCHAR(512) NOT NULL,
    program_name TEXT NOT NULL,
    event_name TEXT NOT NULL,
    visual_keywords TEXT NOT NULL,
    concept_description TEXT NOT NULL,
    create_at DATE NOT NULL,
    FOREIGN KEY (p_no) REFERENCES project(p_no)
);

/* ============================================================
   12. poster_reference_data (포스터 참조데이터)
============================================================ */
CREATE TABLE poster_reference_data (
    poster_reference_data_no INT AUTO_INCREMENT PRIMARY KEY,
    year INT NOT NULL,
    region VARCHAR(512) NOT NULL,
    festival_name VARCHAR(512) NOT NULL,
    image_path VARCHAR(512) NOT NULL,
    evaluated_at DATE NOT NULL
);

/* ============================================================
   13. poster_score (포스터 점수) - poster_reference_data FK
============================================================ */
CREATE TABLE poster_score (
    poster_score_no INT AUTO_INCREMENT PRIMARY KEY,
    poster_reference_data_no INT NOT NULL,
    aesthetic DECIMAL(10,1) NOT NULL,
    aesthetic_description TEXT NOT NULL,
    thematic DECIMAL(10,1) NOT NULL,
    thematic_description TEXT NOT NULL,
    readability DECIMAL(10,1) NOT NULL,
    readability_description TEXT NOT NULL,
    creativity DECIMAL(10,1) NOT NULL,
    creativity_description TEXT NOT NULL,
    total_score DECIMAL(10,1) NOT NULL,
    FOREIGN KEY (poster_reference_data_no) REFERENCES poster_reference_data(poster_reference_data_no)
);

/* ============================================================
   14. festival_category (축제 카테고리)
============================================================ */
CREATE TABLE festival_category (
    festival_category_no INT AUTO_INCREMENT PRIMARY KEY,
    category VARCHAR(512) NOT NULL
);

/* ============================================================
   15. cardnews_reference (카드뉴스 참조데이터)
============================================================ */
CREATE TABLE cardnews_reference (
    cardnews_reference_no INT AUTO_INCREMENT PRIMARY KEY,
    file_path_no INT NOT NULL,
    festival_category_no INT NOT NULL,
    original_link VARCHAR(512) NOT NULL,
    FOREIGN KEY (file_path_no) REFERENCES file_path(file_path_no),
    FOREIGN KEY (festival_category_no) REFERENCES festival_category(festival_category_no)
);

/* ============================================================
   16. cardnews_score (카드뉴스 점수) – 위의 cardnews_reference FK
============================================================ */
CREATE TABLE cardnews_score (
    cardnews_score_no INT AUTO_INCREMENT PRIMARY KEY,
    cardnews_reference_no INT NOT NULL,
    clarity_score DECIMAL(10,1),
    clarity_description TEXT,
    constrast_score DECIMAL(10,1),
    constrast_description TEXT,
    distraction_score DECIMAL(10,1),
    distraction_description TEXT,
    color_harmony_score DECIMAL(10,1),
    color_harmony_description TEXT,
    balance_score DECIMAL(10,1),
    balance_description TEXT,
    semantic_fit_score DECIMAL(10,1),
    semantic_fit_description TEXT,
    create_at DATE NOT NULL,
    total_score DECIMAL(10,1) NOT NULL,
    FOREIGN KEY (cardnews_reference_no)
        REFERENCES cardnews_reference(cardnews_reference_no)
);

/* ============================================================
   17. plan_anlazy (기획서 분석 데이터)
============================================================ */
CREATE TABLE plan_anlazy (
    plan_anlazy_no INT AUTO_INCREMENT PRIMARY KEY,
    anlazy_result JSON NOT NULL
);

/* ============================================================
   18. live_poster (라이브 포스터) – promotion FK, project FK
============================================================ */
CREATE TABLE live_poster (
    live_poster_no INT AUTO_INCREMENT PRIMARY KEY,
    poster_no INT NOT NULL,
    task_id VARCHAR(512) NOT NULL,
    file_path VARCHAR(512) NOT NULL,
    motion_prompt TEXT NOT NULL,
    create_at DATETIME NOT NULL,
    p_no INT NOT NULL,
    FOREIGN KEY (poster_no) REFERENCES promotion(promotion_no),
    FOREIGN KEY (p_no) REFERENCES project(p_no)
);

/* ============================================================
   19. editor_template (에디터 템플릿) – project FK
============================================================ */
CREATE TABLE editor_template (
    editor_template_id INT AUTO_INCREMENT PRIMARY KEY,
    p_no INT NOT NULL,
    file_path VARCHAR(512) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (p_no) REFERENCES project(p_no)
);

/* ============================================================
   20. promotion_path (홍보물 경로) – project FK
============================================================ */
CREATE TABLE promotion_path (
    promotion_path_no INT AUTO_INCREMENT PRIMARY KEY,
    p_no INT NOT NULL,
    db_file_path VARCHAR(512) NOT NULL,
    db_file_type VARCHAR(512) NOT NULL,
    create_at DATE NOT NULL,
    FOREIGN KEY (p_no) REFERENCES project(p_no)
);

