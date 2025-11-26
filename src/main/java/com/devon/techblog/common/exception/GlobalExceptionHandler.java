package com.devon.techblog.common.exception;

import com.devon.techblog.common.dto.api.ApiResponse;
import com.devon.techblog.common.dto.api.FieldError;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusinessException(CustomException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.error("[CustomException] {} - {}", errorCode.name(), e.getMessage(), e);
        ApiResponse<Object> response = ApiResponse.failure(errorCode.getMessage());
        return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ApiResponse<Object>> handleValidationException(BindException e) {
        log.error("[ValidationException] {}", e.getMessage(), e);

        List<FieldError> errors = e.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> FieldError.of(
                        fieldError.getField(),
                        fieldError.getDefaultMessage(),
                        fieldError.getRejectedValue()
                ))
                .toList();

        ApiResponse<Object> response = ApiResponse.failure("validation_failed", errors);
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<ApiResponse<Object>> handleExpiredJwtException(ExpiredJwtException e) {
        log.error("[ExpiredJwtException] {}", e.getMessage());
        ApiResponse<Object> response = ApiResponse.failure("토큰이 만료되었습니다");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ApiResponse<Object>> handleJwtException(JwtException e) {
        log.error("[JwtException] {}", e.getMessage());
        ApiResponse<Object> response = ApiResponse.failure("유효하지 않은 토큰입니다");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleException(Exception e) {
        log.error("[UnhandledException] {}", e.getMessage(), e);
        ApiResponse<Object> response = ApiResponse.failure("internal_server_error");
        return ResponseEntity.internalServerError().body(response);
    }
}