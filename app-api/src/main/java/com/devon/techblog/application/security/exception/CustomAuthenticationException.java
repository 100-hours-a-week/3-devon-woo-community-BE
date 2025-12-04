package com.devon.techblog.application.security.exception;

import com.devon.techblog.common.exception.ErrorCode;
import org.springframework.security.core.AuthenticationException;

public class CustomAuthenticationException extends AuthenticationException {

    private final ErrorCode errorCode;

    public CustomAuthenticationException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public CustomAuthenticationException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
