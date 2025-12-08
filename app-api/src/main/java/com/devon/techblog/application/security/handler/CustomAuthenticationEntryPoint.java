package com.devon.techblog.application.security.handler;

import com.devon.techblog.application.security.util.SecurityResponseSender;
import com.devon.techblog.common.exception.code.AuthErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final SecurityResponseSender securityResponseSender;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        log.error("[AuthenticationEntryPoint] {}", authException.getMessage(), authException);

        securityResponseSender.sendError(response, AuthErrorCode.AUTHENTICATION_REQUIRED);
    }
}
