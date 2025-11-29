package com.devon.techblog.application.security.util;

import com.devon.techblog.application.security.config.properties.JwtProperties;
import com.devon.techblog.application.security.constants.CookieConstants;
import com.devon.techblog.application.security.constants.SecurityConstants;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CookieProvider {

    private final JwtProperties jwtProperties;

    public void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        long maxAgeInSeconds = jwtProperties.getRefreshTokenExpiration() / 1000;
        addCookie(response, CookieConstants.REFRESH_TOKEN_COOKIE_NAME, refreshToken,
                  SecurityConstants.REFRESH_TOKEN_URL, maxAgeInSeconds);
    }

    public Optional<String> getRefreshTokenFromCookie(HttpServletRequest request) {
        return getCookie(request, CookieConstants.REFRESH_TOKEN_COOKIE_NAME);
    }

    public void deleteRefreshTokenCookie(HttpServletResponse response) {
        deleteCookie(response, CookieConstants.REFRESH_TOKEN_COOKIE_NAME, SecurityConstants.REFRESH_TOKEN_URL);
    }

    public static void addOAuth2AuthorizationRequestCookie(HttpServletResponse response, String value) {
        addCookie(response, CookieConstants.OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME, value,
                  "/", CookieConstants.OAUTH2_COOKIE_EXPIRE_SECONDS);
    }

    public static Optional<String> getOAuth2AuthorizationRequestCookie(HttpServletRequest request) {
        return getCookie(request, CookieConstants.OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
    }

    public static void deleteOAuth2AuthorizationRequestCookie(HttpServletResponse response) {
        deleteCookie(response, CookieConstants.OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME, "/");
    }

    public static void addRedirectUriCookie(HttpServletResponse response, String redirectUri) {
        addCookie(response, CookieConstants.REDIRECT_URI_COOKIE_NAME, redirectUri,
                  "/", CookieConstants.OAUTH2_COOKIE_EXPIRE_SECONDS);
    }

    public static Optional<String> getRedirectUriCookie(HttpServletRequest request) {
        return getCookie(request, CookieConstants.REDIRECT_URI_COOKIE_NAME);
    }

    public static void deleteRedirectUriCookie(HttpServletResponse response) {
        deleteCookie(response, CookieConstants.REDIRECT_URI_COOKIE_NAME, "/");
    }

    private static void addCookie(HttpServletResponse response, String name, String value, String path, long maxAge) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                //.secure(true) todo: https 도입후 활성화 할 것
                .httpOnly(true)
                .path(path)
                .maxAge(maxAge)
                .sameSite(CookieConstants.SAME_SITE_STRICT)
                .build();

        response.addHeader(CookieConstants.SET_COOKIE_HEADER, cookie.toString());
    }

    private static Optional<String> getCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        return Arrays.stream(request.getCookies())
                .filter(cookie -> name.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }

    private static void deleteCookie(HttpServletResponse response, String name, String path) {
        ResponseCookie cookie = ResponseCookie.from(name, "")
                .httpOnly(true)
                .path(path)
                .maxAge(0)
                .sameSite(CookieConstants.SAME_SITE_STRICT)
                .build();

        response.addHeader(CookieConstants.SET_COOKIE_HEADER, cookie.toString());
    }
}
