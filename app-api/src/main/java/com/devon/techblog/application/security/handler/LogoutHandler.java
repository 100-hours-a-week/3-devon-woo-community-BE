package com.devon.techblog.application.security.handler;

import com.devon.techblog.application.security.service.TokenBlacklistService;
import com.devon.techblog.application.security.util.CookieProvider;
import com.devon.techblog.application.security.util.SecurityResponseSender;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LogoutHandler {

    private final SecurityResponseSender securityResponseSender;
    private final CookieProvider cookieProvider;
    private final TokenBlacklistService tokenBlacklistService;

    public void onLogout(HttpServletRequest request, HttpServletResponse response) throws IOException {

        SecurityContextHolder.clearContext();

        cookieProvider.getRefreshTokenFromCookie(request)
                .ifPresent(tokenBlacklistService::addToBlacklist);

        cookieProvider.deleteRefreshTokenCookie(response);

        securityResponseSender.sendSuccess(response, HttpServletResponse.SC_OK, "로그아웃되었습니다.");
    }
}
