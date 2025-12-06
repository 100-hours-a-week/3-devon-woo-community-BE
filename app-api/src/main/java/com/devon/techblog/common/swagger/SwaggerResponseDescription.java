package com.devon.techblog.common.swagger;

import com.devon.techblog.common.exception.ErrorCode;
import com.devon.techblog.common.exception.code.CommentErrorCode;
import com.devon.techblog.common.exception.code.CommonErrorCode;
import com.devon.techblog.common.exception.code.MemberErrorCode;
import com.devon.techblog.common.exception.code.PostErrorCode;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Getter;

/**
 * Swagger API 응답 설명을 위한 Enum
 * 각 API별로 발생 가능한 에러 코드를 정의
 */
@Getter
public enum SwaggerResponseDescription {

    // Auth API
    AUTH_SIGNUP(new LinkedHashSet<>(Set.of(
            MemberErrorCode.INVALID_EMAIL_FORMAT,
            MemberErrorCode.INVALID_PASSWORD_FORMAT,
            MemberErrorCode.INVALID_NICKNAME,
            MemberErrorCode.DUPLICATE_EMAIL,
            MemberErrorCode.DUPLICATE_NICKNAME
    ))),
    AUTH_LOGIN(new LinkedHashSet<>(Set.of(
            MemberErrorCode.INVALID_EMAIL_FORMAT,
            MemberErrorCode.USER_NOT_FOUND,
            MemberErrorCode.INVALID_PASSWORD
    ))),
    AUTH_LOGOUT(new LinkedHashSet<>(Set.of())),

    // Member API
    MEMBER_GET(new LinkedHashSet<>(Set.of(
            MemberErrorCode.USER_NOT_FOUND
    ))),
    MEMBER_UPDATE(new LinkedHashSet<>(Set.of(
            MemberErrorCode.USER_NOT_FOUND,
            MemberErrorCode.INVALID_NICKNAME,
            MemberErrorCode.DUPLICATE_NICKNAME
    ))),
    MEMBER_PASSWORD_UPDATE(new LinkedHashSet<>(Set.of(
            MemberErrorCode.USER_NOT_FOUND,
            MemberErrorCode.INVALID_PASSWORD_FORMAT,
            MemberErrorCode.INVALID_CURRENT_PASSWORD,
            MemberErrorCode.SAME_AS_CURRENT_PASSWORD
    ))),
    MEMBER_DELETE(new LinkedHashSet<>(Set.of(
            MemberErrorCode.USER_NOT_FOUND
    ))),

    // Post API
    POST_CREATE(new LinkedHashSet<>(Set.of(
            MemberErrorCode.USER_NOT_FOUND
    ))),
    POST_UPDATE(new LinkedHashSet<>(Set.of(
            PostErrorCode.POST_NOT_FOUND,
            PostErrorCode.NO_PERMISSION
    ))),
    POST_DELETE(new LinkedHashSet<>(Set.of(
            PostErrorCode.POST_NOT_FOUND,
            PostErrorCode.NO_PERMISSION
    ))),
    POST_GET(new LinkedHashSet<>(Set.of(
            PostErrorCode.POST_NOT_FOUND
    ))),
    POST_LIST(new LinkedHashSet<>(Set.of())),
    POST_LIKE(new LinkedHashSet<>(Set.of(
            PostErrorCode.POST_NOT_FOUND,
            MemberErrorCode.USER_NOT_FOUND,
            PostErrorCode.ALREADY_LIKED
    ))),
    POST_UNLIKE(new LinkedHashSet<>(Set.of(
            PostErrorCode.POST_NOT_FOUND,
            MemberErrorCode.USER_NOT_FOUND,
            PostErrorCode.LIKE_NOT_FOUND
    ))),

    // Comment API
    COMMENT_CREATE(new LinkedHashSet<>(Set.of(
            PostErrorCode.POST_NOT_FOUND,
            MemberErrorCode.USER_NOT_FOUND
    ))),
    COMMENT_LIST(new LinkedHashSet<>(Set.of(
            PostErrorCode.POST_NOT_FOUND
    ))),
    COMMENT_GET(new LinkedHashSet<>(Set.of(
            CommentErrorCode.COMMENT_NOT_FOUND
    ))),
    COMMENT_UPDATE(new LinkedHashSet<>(Set.of(
            CommentErrorCode.COMMENT_NOT_FOUND,
            CommentErrorCode.NO_PERMISSION
    ))),
    COMMENT_DELETE(new LinkedHashSet<>(Set.of(
            CommentErrorCode.COMMENT_NOT_FOUND,
            CommentErrorCode.NO_PERMISSION
    )));

    private final Set<ErrorCode> errorCodeList;

    SwaggerResponseDescription(Set<ErrorCode> errorCodeList) {
        // 공통 에러 추가
        errorCodeList.addAll(new LinkedHashSet<>(Set.of(
                CommonErrorCode.INVALID_REQUEST,
                CommonErrorCode.INTERNAL_SERVER_ERROR
        )));

        this.errorCodeList = errorCodeList;
    }
}
