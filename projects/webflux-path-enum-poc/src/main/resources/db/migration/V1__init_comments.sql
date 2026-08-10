-- V1__init_comments.sql
-- Path Enumeration 방식 무한 depth 댓글 시스템의 핵심 테이블.
--
-- ⚠️ 핵심 제약:
--   - path 컬럼은 utf8mb4_bin collation 필수 (대소문자 구분 비교를 위해).
--   - 테이블 전체 기본 collation 도 utf8mb4_bin.
--   - 이유: path 값은 base62 5자리 고정폭 세그먼트들의 결합이며,
--           "0-9A-Za-z" ASCII 코드 포인트 순서대로 정렬/비교되어야 한다.

CREATE TABLE comments (
    id          BIGINT       NOT NULL PRIMARY KEY,
    post_id     BIGINT       NOT NULL,
    parent_id   BIGINT       NULL,
    path        VARCHAR(255) NOT NULL COLLATE utf8mb4_bin,
    depth       INT          NOT NULL,
    content     TEXT         NOT NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NULL ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_comments_parent FOREIGN KEY (parent_id) REFERENCES comments(id) ON DELETE CASCADE,

    INDEX idx_post_path (post_id, path),
    INDEX idx_parent (parent_id),
    INDEX idx_depth (post_id, depth),

    UNIQUE KEY uk_post_path (post_id, path)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
