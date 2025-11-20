package com.kakaotechbootcamp.community.application.security.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.List;

@Slf4j
@Component
public class RedirectUriValidator {

    private final List<String> allowedOrigins;

    public RedirectUriValidator(
            @Value("${spring.security.cors.allowed-origins}") List<String> allowedOrigins
    ) {
        this.allowedOrigins = allowedOrigins;
    }

    public boolean isValidRedirectUri(String redirectUri) {
        if (redirectUri == null || redirectUri.isBlank()) {
            return false;
        }

        try {
            URI uri = URI.create(redirectUri);
            String origin = uri.getScheme() + "://" + uri.getHost() +
                           (uri.getPort() != -1 ? ":" + uri.getPort() : "");

            boolean valid = allowedOrigins.contains(origin);

            if (!valid) {
                log.warn("Invalid redirect URI rejected: {}", redirectUri);
            }

            return valid;
        } catch (Exception e) {
            log.warn("Invalid redirect URI format: {}", redirectUri, e);
            return false;
        }
    }
}
