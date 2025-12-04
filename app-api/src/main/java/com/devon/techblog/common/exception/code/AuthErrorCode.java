package com.devon.techblog.common.exception.code;

import com.devon.techblog.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {

    AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "인증 정보가 유효하지 않습니다"),

    TOKEN_MISSING(HttpStatus.UNAUTHORIZED, "토큰이 제공되지 않았습니다"),
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다"),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다"),
    TOKEN_MALFORMED(HttpStatus.UNAUTHORIZED, "잘못된 형식의 토큰입니다"),
    TOKEN_UNSUPPORTED(HttpStatus.UNAUTHORIZED, "지원하지 않는 토큰 형식입니다"),
    TOKEN_SIGNATURE_INVALID(HttpStatus.UNAUTHORIZED, "토큰 서명 검증에 실패했습니다"),

    REFRESH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다"),
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 만료되었습니다"),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "리프레시 토큰을 찾을 수 없습니다"),

    OAUTH2_AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "OAuth2 인증에 실패했습니다"),
    OAUTH2_PROVIDER_ERROR(HttpStatus.BAD_GATEWAY, "OAuth2 제공자 오류가 발생했습니다"),
    OAUTH2_USER_INFO_ERROR(HttpStatus.BAD_GATEWAY, "사용자 정보를 가져올 수 없습니다"),
    OAUTH2_INVALID_STATE(HttpStatus.BAD_REQUEST, "잘못된 state 파라미터입니다"),
    OAUTH2_INVALID_CODE(HttpStatus.BAD_REQUEST, "잘못된 authorization code입니다"),

    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다"),
    INSUFFICIENT_PERMISSIONS(HttpStatus.FORBIDDEN, "권한이 부족합니다"),
    ROLE_NOT_FOUND(HttpStatus.FORBIDDEN, "사용자 권한을 찾을 수 없습니다"),

    ACCOUNT_LOCKED(HttpStatus.FORBIDDEN, "계정이 잠겼습니다"),
    ACCOUNT_DISABLED(HttpStatus.FORBIDDEN, "비활성화된 계정입니다"),
    ACCOUNT_EXPIRED(HttpStatus.FORBIDDEN, "만료된 계정입니다"),
    CREDENTIALS_EXPIRED(HttpStatus.FORBIDDEN, "자격 증명이 만료되었습니다");

    private final HttpStatus httpStatus;
    private final String message;
}

