-- 테스트용 회원 데이터
INSERT INTO member (email, password, nickname, profile_image_url, role, status)
VALUES ('test1@example.com', 'password123', 'tester1', 'https://example.com/profile1.png', 'USER', 'ACTIVE');

INSERT INTO member (email, password, nickname, profile_image_url, role, status)
VALUES ('test2@example.com', 'password123', 'tester2', 'https://example.com/profile2.png', 'USER', 'ACTIVE');

INSERT INTO member (email, password, nickname, profile_image_url, role, status)
VALUES ('admin@example.com', 'admin123', 'admin', 'https://example.com/admin.png', 'ADMIN', 'ACTIVE');
