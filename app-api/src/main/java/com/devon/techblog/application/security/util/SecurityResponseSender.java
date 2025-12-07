package com.devon.techblog.application.security.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.devon.techblog.common.dto.api.ApiResponse;
import com.devon.techblog.common.dto.api.ErrorResponse;
import com.devon.techblog.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/**
 * Spring Security 필터/핸들러에서 사용하는 공통 응답 전송 유틸리티.
 * <p>
 * - JSON 헤더 설정
 * - 상태 코드 설정
 * - {@link ApiResponse} / {@link ErrorResponse} 직렬화
 */
@Component
@RequiredArgsConstructor
public class SecurityResponseSender {

    private final ObjectMapper objectMapper;

    public void sendError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ErrorResponse errorResponse = ErrorResponse.of(message);
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

    public void sendError(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ErrorResponse errorResponse = ErrorResponse.from(errorCode);
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

    public void sendSuccess(HttpServletResponse response, int status, String message) throws IOException {
        sendSuccess(response, status, null, message);
    }

    public <T> void sendSuccess(HttpServletResponse response, int status, T body, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiResponse<T> apiResponse = ApiResponse.success(body, message);
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}
