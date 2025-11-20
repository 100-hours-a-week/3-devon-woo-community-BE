package com.kakaotechbootcamp.community.application.security.util;

import com.kakaotechbootcamp.community.application.security.config.CorsProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.List;

@Slf4j
@Component
public class RedirectUriValidator {

    private final List<String> allowedOrigins;

    public RedirectUriValidator(CorsProperties corsProperties) {
        this.allowedOrigins = corsProperties.getAllowedOrigins();
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
