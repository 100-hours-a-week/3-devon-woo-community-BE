package com.devon.techblog.infra.image.cloudinary.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "storage.cloudinary", name = "enabled", havingValue = "true")
public class CloudinaryConfig {

    @Value("${storage.cloudinary.cloud.name}")
    private String cloudName;

    @Value("${storage.cloudinary.api.key}")
    private String apiKey;

    @Value("${storage.cloudinary.api.secret}")
    private String apiSecret;

    @Bean
    public Cloudinary cloudinary() {
        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret
        ));
    }
}
