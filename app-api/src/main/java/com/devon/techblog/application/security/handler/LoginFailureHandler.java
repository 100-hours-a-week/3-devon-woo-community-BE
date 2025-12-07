package com.devon.techblog.application.security.handler;

import com.devon.techblog.application.security.util.SecurityResponseSender;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoginFailureHandler implements AuthenticationFailureHandler {

    private final SecurityResponseSender securityResponseSender;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {

        log.info("로그인 실패: {}", exception.getMessage());

        securityResponseSender.sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "로그인이 실패했습니다");
    }
}
