package com.devon.techblog.application.security.util;

import com.devon.techblog.application.security.config.properties.CorsProperties;
import java.net.URI;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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
