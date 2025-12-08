package com.devon.techblog.common.exception.http;

import com.devon.techblog.common.exception.BusinessException;
import com.devon.techblog.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public class ConflictException extends BusinessException {

    public ConflictException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ConflictException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.CONFLICT;
    }
}
