package com.devon.techblog.application.security.handler;

import com.devon.techblog.application.security.dto.response.LoginResponse;
import com.devon.techblog.application.security.dto.user.CustomUserDetails;
import com.devon.techblog.application.security.util.CookieProvider;
import com.devon.techblog.application.security.util.JwtTokenProvider;
import com.devon.techblog.common.dto.api.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final JwtTokenProvider jwtTokenProvider;
    private final CookieProvider cookieProvider;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        log.info("로그인 성공: {}", authentication.getName());

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getUid();
        String role = userDetails.getRole();

        String accessToken = jwtTokenProvider.generateAccessToken(userId, role);
        String refreshToken = jwtTokenProvider.generateRefreshToken(userId);

        cookieProvider.addRefreshTokenCookie(response, refreshToken);

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        LoginResponse loginResponse = new LoginResponse(userId, accessToken);
        ApiResponse<LoginResponse> apiResponse = ApiResponse.success(loginResponse, "로그인이 성공했습니다");
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}
