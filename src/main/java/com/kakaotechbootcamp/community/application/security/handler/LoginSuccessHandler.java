package com.kakaotechbootcamp.community.application.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kakaotechbootcamp.community.application.security.dto.CustomUserDetails;
import com.kakaotechbootcamp.community.application.security.dto.LoginResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        log.info("로그인 성공: {}", authentication.getName());

        // CustomUserDetails에서 userId 추출
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getMemberId();

        // 응답 설정
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // LoginResponse 생성 및 반환
        LoginResponse loginResponse = new LoginResponse(userId);
        response.getWriter().write(objectMapper.writeValueAsString(loginResponse));
    }
}
