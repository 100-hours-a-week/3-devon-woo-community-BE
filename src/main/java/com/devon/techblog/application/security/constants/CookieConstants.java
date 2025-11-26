package com.devon.techblog.application.security.constants;

public class CookieConstants {
    public static final String SET_COOKIE_HEADER = "Set-Cookie";
    public static final String SAME_SITE_STRICT = "Strict";

    // Refresh 쿠키
    public static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

    // OAuth2 관련 쿠키
    public static final String OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME = "oauth2_auth_request";
    public static final String REDIRECT_URI_COOKIE_NAME = "redirect_uri";
    public static final int OAUTH2_COOKIE_EXPIRE_SECONDS = 180;
}
