package com.devon.techblog.common.exception.code;

import com.devon.techblog.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CommentErrorCode implements ErrorCode {

    // 댓글 조회 에러 (404)
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다"),

    // 권한 에러 (403)
    NO_PERMISSION(HttpStatus.FORBIDDEN, "권한이 없습니다"),

    // 댓글 생성/수정 에러 (400)
    COMMENT_POST_REQUIRED(HttpStatus.BAD_REQUEST, "게시글은 필수입니다"),
    COMMENT_MEMBER_REQUIRED(HttpStatus.BAD_REQUEST, "작성자는 필수입니다"),
    COMMENT_CONTENT_REQUIRED(HttpStatus.BAD_REQUEST, "댓글 내용은 필수입니다");

    private final HttpStatus httpStatus;
    private final String message;
}
