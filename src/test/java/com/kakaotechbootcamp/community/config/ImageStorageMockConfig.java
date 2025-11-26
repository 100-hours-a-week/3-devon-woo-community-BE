package com.kakaotechbootcamp.community.config;

import com.kakaotechbootcamp.community.fake.FakeImageStorageService;
import com.kakaotechbootcamp.community.infra.image.ImageSignature;
import com.kakaotechbootcamp.community.infra.image.ImageStorageService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * 통합 테스트에서 실제 Cloudinary 등 외부 이미지 스토리지를 호출하지 않도록
 * 고정된 서명/타임스탬프를 반환하는 테스트용 FakeImageStorageService 등록
 */
@TestConfiguration
public class ImageStorageMockConfig {

    @Bean
    @Primary
    public ImageStorageService testImageStorageService() {
        return new FakeImageStorageService();
    }
}
