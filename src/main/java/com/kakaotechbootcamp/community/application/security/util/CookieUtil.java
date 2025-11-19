package com.kakaotechbootcamp.community.application.security.util;

import com.kakaotechbootcamp.community.application.security.config.JwtProperties;
import com.kakaotechbootcamp.community.application.security.constants.CookieConstants;
import com.kakaotechbootcamp.community.application.security.constants.SecurityConstants;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CookieUtil {

    private final JwtProperties jwtProperties;

    public void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        long maxAgeInSeconds = jwtProperties.getRefreshTokenExpiration() / 1000;

        ResponseCookie cookie = ResponseCookie.from(CookieConstants.REFRESH_TOKEN_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(true)
                .path(SecurityConstants.REFRESH_TOKEN_URL)
                .maxAge(maxAgeInSeconds)
                .sameSite(CookieConstants.SAME_SITE_STRICT)
                .build();

        response.addHeader(CookieConstants.SET_COOKIE_HEADER, cookie.toString());
    }

    public Optional<String> getRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }

        return Arrays.stream(request.getCookies())
                .filter(cookie -> CookieConstants.REFRESH_TOKEN_COOKIE_NAME.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }

    public void deleteRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(CookieConstants.REFRESH_TOKEN_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(true)
                .path(SecurityConstants.REFRESH_TOKEN_URL)
                .maxAge(0)
                .sameSite(CookieConstants.SAME_SITE_STRICT)
                .build();

        response.addHeader(CookieConstants.SET_COOKIE_HEADER, cookie.toString());
    }
}
