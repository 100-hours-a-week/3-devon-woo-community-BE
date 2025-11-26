package com.devon.techblog.application.security.exception;

import com.devon.techblog.common.exception.ErrorCode;
import org.springframework.security.access.AccessDeniedException;

public class CustomAccessDeniedException extends AccessDeniedException {

    private final ErrorCode errorCode;

    public CustomAccessDeniedException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public CustomAccessDeniedException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
