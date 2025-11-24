package com.kakaotechbootcamp.community.config;

import com.kakaotechbootcamp.community.infra.image.ImageSignature;
import com.kakaotechbootcamp.community.infra.image.ImageStorageService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class ImageStorageMockConfig {

    private static final long FIXED_TIMESTAMP = 1735689600L;

    @Bean
    @Primary
    public ImageStorageService testImageStorageService() {
        return type -> new ImageSignature(
                "test-api-key",
                "test-cloud-name",
                FIXED_TIMESTAMP,
                "test-signature",
                "test-upload-preset",
                "profile".equals(type) ? "profiles" : "posts"
        );
    }
}
