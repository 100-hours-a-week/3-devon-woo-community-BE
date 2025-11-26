package com.devon.techblog.common.validation;

public class ValidationMessages {

    // 공통 메시지
    public static final String REQUIRED_FIELD = "필수 입력 항목입니다";
    public static final String INVALID_REQUEST = "잘못된 요청입니다";

    // 이메일 관련
    public static final String INVALID_EMAIL_FORMAT = "올바른 이메일 형식이 아닙니다";

    // 비밀번호 관련
    public static final String INVALID_PASSWORD_FORMAT = "비밀번호는 최소 8자 이상이어야 합니다";
    public static final String REQUIRED_PASSWORD = "비밀번호를 입력해주세요";

    // 닉네임 관련
    public static final String INVALID_NICKNAME = "닉네임은 최대 10자까지 입력 가능합니다";

    // 이미지 URL 관련
    public static final String INVALID_IMAGE_URL = "올바른 이미지 URL 형식이 아닙니다";
    public static final String INVALID_PROFILE_IMAGE = "올바른 프로필 이미지 URL 형식이 아닙니다";

    // 게시글 관련
    public static final String REQUIRED_POST_TITLE = "게시글 제목을 입력해주세요";
    public static final String REQUIRED_POST_CONTENT = "게시글 내용을 입력해주세요";

    // 댓글 관련
    public static final String REQUIRED_COMMENT_CONTENT = "댓글 내용을 입력해주세요";

    // ID 관련
    public static final String REQUIRED_MEMBER_ID = "작성자 정보가 필요합니다";

    private ValidationMessages() {
        // 인스턴스 생성 방지
    }
}
