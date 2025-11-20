-- 테스트 회원 데이터
INSERT INTO member (id, email, password, nickname, profile_image_url, status, role, last_login_at)
VALUES
    (1, 'test1@example.com', 'password123!', 'tester1', 'https://example.com/profile1.jpg', 'ACTIVE', 'USER', NULL),
    (2, 'test2@example.com', 'password123!', 'tester2', 'https://example.com/profile2.jpg', 'ACTIVE', 'USER', NULL);
