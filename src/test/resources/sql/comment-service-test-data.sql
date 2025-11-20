-- 테스트 회원 데이터
INSERT INTO member (id, email, password, nickname, profile_image_url, status, role, last_login_at)
VALUES
    (1, 'test1@example.com', 'password123!', 'tester1', 'https://example.com/profile1.jpg', 'ACTIVE', 'USER' , NULL),
    (2, 'test2@example.com', 'password123!', 'tester2', 'https://example.com/profile2.jpg', 'ACTIVE', 'USER' , NULL);

-- 테스트 게시글 데이터
INSERT INTO post (id, member_id, title, content, views_count, like_count, comment_count, is_deleted, created_at, updated_at)
VALUES
    (1, 1, 'Test Post 1', 'Test Content 1', 0, 0, 2, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 2, 'Test Post 2', 'Test Content 2', 0, 0, 0, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 테스트 댓글 데이터
INSERT INTO comment (id, post_id, member_id, content, is_deleted, created_at, updated_at)
VALUES
    (1, 1, 1, 'Test Comment 1', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 1, 2, 'Test Comment 2', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
