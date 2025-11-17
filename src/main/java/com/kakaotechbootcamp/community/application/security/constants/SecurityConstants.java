package com.kakaotechbootcamp.community.application.security.constants;

public class SecurityConstants {
    public static final String[] PUBLIC_URLS = {
            "/", "/login",
            "/auth/*/logout", "/auth/*/reissue",
            "/oauth2/**", "/api/*",
            "/v3/api-docs/**", "/swagger-ui/**", "/swagger-resources/**", "/actuator/**"
    };

    public static final String[] SECURE_URLS = {
            "/api/*/secure/**", "/api/*/user/**"
    };

    public static final String[] ADMIN_URLS = {
            "/api/*/admin/**", "/admin/**"
    };
}