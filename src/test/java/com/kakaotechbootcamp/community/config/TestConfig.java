package com.kakaotechbootcamp.community.config;

import com.kakaotechbootcamp.community.infra.image.ImageSignature;
import com.kakaotechbootcamp.community.infra.image.ImageStorageService;
import java.time.Instant;
import java.util.Optional;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@TestConfiguration
@EnableJpaAuditing(dateTimeProviderRef = "testDateTimeProvider")
public class TestConfig {

    @Bean
    public DateTimeProvider testDateTimeProvider() {
        return () -> Optional.of(Instant.now());
    }

    @Bean
    @Primary
    public ImageStorageService testImageStorageService() {
        return type -> new ImageSignature(
                "test-api-key",
                "test-cloud-name",
                System.currentTimeMillis() / 1000L,
                "test-signature",
                "test-upload-preset",
                "profile".equals(type) ? "profiles" : "posts"
        );
    }
}
