package com.devon.techblog.common.exception.code;

import com.devon.techblog.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PostErrorCode implements ErrorCode {

    // 게시글 조회 에러 (404)
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다"),

    // 권한 에러 (403)
    NO_PERMISSION(HttpStatus.FORBIDDEN, "권한이 없습니다"),

    // 좋아요 관련 에러
    ALREADY_LIKED(HttpStatus.CONFLICT, "이미 좋아요를 눌렀습니다"),
    LIKE_NOT_FOUND(HttpStatus.NOT_FOUND, "좋아요를 찾을 수 없습니다"),

    // 게시글 생성/수정 에러 (400)
    POST_MEMBER_REQUIRED(HttpStatus.BAD_REQUEST, "작성자는 필수입니다"),
    POST_TITLE_REQUIRED(HttpStatus.BAD_REQUEST, "제목은 필수입니다"),
    POST_TITLE_TOO_LONG(HttpStatus.BAD_REQUEST, "제목은 200자를 초과할 수 없습니다"),
    POST_CONTENT_REQUIRED(HttpStatus.BAD_REQUEST, "내용은 필수입니다"),

    // 첨부파일 에러 (400)
    ATTACHMENT_POST_REQUIRED(HttpStatus.BAD_REQUEST, "게시글은 필수입니다"),
    ATTACHMENT_URL_REQUIRED(HttpStatus.BAD_REQUEST, "첨부파일 URL은 필수입니다"),
    ATTACHMENT_URL_TOO_LONG(HttpStatus.BAD_REQUEST, "첨부파일 URL은 500자를 초과할 수 없습니다");

    private final HttpStatus httpStatus;
    private final String message;
}
