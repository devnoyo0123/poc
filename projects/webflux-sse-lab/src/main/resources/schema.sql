-- R2DBC Test Schema for Relationships
-- WebFlux + R2DBC 조인 실습용 스키마

-- Users Table
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE
);

-- Departments Table (One-to-One with Users)
CREATE TABLE IF NOT EXISTS departments (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    user_id BIGINT UNIQUE REFERENCES users(id)  -- 담당자 (1:1)
);

-- Posts Table (Many-to-One with Users)
CREATE TABLE IF NOT EXISTS posts (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    content TEXT,
    user_id BIGINT NOT NULL REFERENCES users(id),  -- 작성자 (N:1)
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tags Table
CREATE TABLE IF NOT EXISTS tags (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

-- Post Tags Junction Table (Many-to-Many)
CREATE TABLE IF NOT EXISTS post_tags (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    tag_id BIGINT NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
    UNIQUE(post_id, tag_id)
);

-- ==================== Sample Data ====================

-- Users
INSERT INTO users (id, name, email) VALUES
    (1, 'Kim Chul-soo', 'kim@example.com'),
    (2, 'Lee Young-hee', 'lee@example.com'),
    (3, 'Park Min-ji', 'park@example.com')
ON CONFLICT (id) DO NOTHING;

-- Departments (One-to-One)
INSERT INTO departments (id, name, user_id) VALUES
    (1, 'Development Team', 1),  -- Kim Chul-soo가 관리
    (2, 'Design Team', 2),       -- Lee Young-hee가 관리
    (3, 'Marketing Team', NULL)  -- 담당자 없음
ON CONFLICT (id) DO NOTHING;

-- Posts (Many-to-One)
INSERT INTO posts (id, title, content, user_id) VALUES
    (1, 'WebFlux Tutorial', 'WebFlux 기초 학습 가이드', 1),
    (2, 'Kotlin Coroutines', '코루틴 완벽 가이드', 1),
    (3, 'UI/UX Design Principles', '디자인 원칙', 2),
    (4, 'Reactive Programming', '리액티브 프로그래밍 입문', 3),
    (5, 'R2DBC vs JPA', 'R2DBC와 JPA 비교', 1)
ON CONFLICT (id) DO NOTHING;

-- Tags
INSERT INTO tags (id, name) VALUES
    (1, 'Kotlin'),
    (2, 'Spring'),
    (3, 'WebFlux'),
    (4, 'R2DBC'),
    (5, 'Design'),
    (6, 'Reactive')
ON CONFLICT (id) DO NOTHING;

-- Post Tags (Many-to-Many)
INSERT INTO post_tags (post_id, tag_id) VALUES
    (1, 1), -- WebFlux Tutorial -> Kotlin
    (1, 2), -- WebFlux Tutorial -> Spring
    (1, 3), -- WebFlux Tutorial -> WebFlux
    (2, 1), -- Kotlin Coroutines -> Kotlin
    (2, 6), -- Kotlin Coroutines -> Reactive
    (3, 5), -- UI/UX Design -> Design
    (4, 3), -- Reactive Programming -> WebFlux
    (4, 6), -- Reactive Programming -> Reactive
    (5, 2), -- R2DBC vs JPA -> Spring
    (5, 4), -- R2DBC vs JPA -> R2DBC
    (5, 6)  -- R2DBC vs JPA -> Reactive
ON CONFLICT (post_id, tag_id) DO NOTHING;

-- ==================== Indexes for Performance ====================

CREATE INDEX IF NOT EXISTS idx_posts_user_id ON posts(user_id);
CREATE INDEX IF NOT EXISTS idx_post_tags_post_id ON post_tags(post_id);
CREATE INDEX IF NOT EXISTS idx_post_tags_tag_id ON post_tags(tag_id);
CREATE INDEX IF NOT EXISTS idx_departments_user_id ON departments(user_id);
