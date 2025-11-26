package com.devon.techblog.common.exception.code;

import com.devon.techblog.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MemberErrorCode implements ErrorCode {

    // 인증 관련 에러 (400/401)
    INVALID_EMAIL_FORMAT(HttpStatus.BAD_REQUEST, "이메일 형식이 올바르지 않습니다"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"),
    INVALID_OR_EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않거나 만료된 토큰입니다"),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "비밀번호가 일치하지 않습니다"),
    INVALID_CURRENT_PASSWORD(HttpStatus.BAD_REQUEST, "현재 비밀번호가 일치하지 않습니다"),

    // 검증 에러 (400)
    INVALID_NICKNAME(HttpStatus.BAD_REQUEST, "닉네임이 유효하지 않습니다"),
    INVALID_PASSWORD_FORMAT(HttpStatus.BAD_REQUEST, "비밀번호 형식이 올바르지 않습니다"),
    MISSING_PASSWORD(HttpStatus.BAD_REQUEST, "비밀번호가 필요합니다"),
    SAME_AS_CURRENT_PASSWORD(HttpStatus.BAD_REQUEST, "현재 비밀번호와 동일합니다"),

    // 중복 에러 (409)
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다"),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다"),

    // 회원 상태 에러 (403)
    MEMBER_INACTIVE(HttpStatus.FORBIDDEN, "비활성화된 회원입니다"),
    MEMBER_WITHDRAWN(HttpStatus.FORBIDDEN, "탈퇴한 회원입니다"),
    MEMBER_CANNOT_LOGIN(HttpStatus.FORBIDDEN, "로그인할 수 없는 상태입니다"),

    // 도메인 검증 에러 (500 - 서버 내부 로직 오류)
    MEMBER_NICKNAME_REQUIRED(HttpStatus.INTERNAL_SERVER_ERROR, "닉네임은 필수입니다 (서버 로직 오류)"),
    MEMBER_NICKNAME_TOO_LONG(HttpStatus.INTERNAL_SERVER_ERROR, "닉네임은 10자를 초과할 수 없습니다 (서버 로직 오류)"),
    MEMBER_PROFILE_IMAGE_URL_TOO_LONG(HttpStatus.INTERNAL_SERVER_ERROR, "프로필 이미지 URL은 500자를 초과할 수 없습니다 (서버 로직 오류)"),
    MEMBER_NO_PROFILE_UPDATE_DATA(HttpStatus.INTERNAL_SERVER_ERROR, "업데이트할 프로필 정보가 없습니다 (서버 로직 오류)");

    private final HttpStatus httpStatus;
    private final String message;
}
