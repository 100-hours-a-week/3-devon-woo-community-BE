package com.devon.techblog.common.exception.handler;

import com.devon.techblog.common.dto.api.ErrorResponse;
import com.devon.techblog.common.dto.api.FieldError;
import com.devon.techblog.common.exception.BusinessException;
import com.devon.techblog.common.exception.ErrorCode;
import com.devon.techblog.common.exception.code.AuthErrorCode;
import com.devon.techblog.common.exception.code.CommonErrorCode;
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

    /**
     * 비즈니스 예외 처리 핸들러.
     * <p>
     * 도메인 로직에서 예상 가능한 오류(검증 실패, 권한 부족 등)를 처리한다.
     * {@link BusinessException} 에 포함된 {@link ErrorCode} 정보를 기반으로
     * HTTP 응답 코드와 에러 응답 바디를 생성한다.
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        String errorMessage = errorCode.getMessage();
        HttpStatus httpStatus = e.getHttpStatus();

        // 비즈니스 예외는 AOP에서 일괄 로깅하며, 여기서는 응답 생성만 담당한다.
        ErrorResponse response = ErrorResponse.from(errorCode);
        return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
    }


    /**
     * 요청 바인딩 / DTO 검증 실패 처리 핸들러.
     * <p>
     * {@link MethodArgumentNotValidException}, {@link BindException} 에서 발생한
     * 필드 단위 오류 정보를 {@link FieldError} 리스트로 변환하여 응답한다.
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ErrorResponse> handleValidationException(BindException e) {
        List<FieldError> errors = e.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> FieldError.of(
                        fieldError.getField(),
                        fieldError.getDefaultMessage(),
                        fieldError.getRejectedValue()
                ))
                .toList();

        ErrorResponse response = ErrorResponse.from(CommonErrorCode.VALIDATION_FAILED, errors);
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 만료된 JWT 토큰 예외 처리 핸들러.
     * <p>
     * 인증 토큰의 유효 기간이 지난 경우 401 응답과 함께 사용자 친화적인 메시지를 내려준다.
     */
    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<ErrorResponse> handleExpiredJwtException(ExpiredJwtException e) {
        ErrorResponse response = ErrorResponse.from(AuthErrorCode.TOKEN_EXPIRED);
        return ResponseEntity.status(AuthErrorCode.TOKEN_EXPIRED.getHttpStatus()).body(response);
    }

    /**
     * 그 외 JWT 관련 예외 처리 핸들러.
     * <p>
     * 토큰 서명 오류, 구조 오류 등 유효하지 않은 토큰에 대해 401 응답을 반환한다.
     */
    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ErrorResponse> handleJwtException(JwtException e) {
        ErrorResponse response = ErrorResponse.from(AuthErrorCode.TOKEN_INVALID);
        return ResponseEntity.status(AuthErrorCode.TOKEN_INVALID.getHttpStatus()).body(response);
    }

    /**
     * 처리되지 않은 모든 예외에 대한 최종 방어선 핸들러.
     * <p>
     * 예상하지 못한 서버 내부 오류를 500 상태 코드와 함께 반환한다.
     * 상세 로깅 및 알림은 AOP 또는 로깅 설정에서 담당하며,
     * 여기서는 클라이언트에 공통 에러 응답을 내려주는 역할만 수행한다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        ErrorResponse response = ErrorResponse.of("서버 내부 오류가 발생했습니다");
        return ResponseEntity.internalServerError().body(response);
    }
}
