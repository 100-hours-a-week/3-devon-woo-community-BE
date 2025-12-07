package com.devon.techblog.common.exception.code;

import com.devon.techblog.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum FileErrorCode implements ErrorCode {

    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "파일을 찾을 수 없습니다"),
    FILE_STORAGE_NOT_CONFIGURED(HttpStatus.SERVICE_UNAVAILABLE, "파일 스토리지 설정이 구성되지 않아 업로드 기능을 사용할 수 없습니다");

    private final HttpStatus httpStatus;
    private final String message;
}
