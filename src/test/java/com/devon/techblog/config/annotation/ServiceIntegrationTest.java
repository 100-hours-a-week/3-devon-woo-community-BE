package com.devon.techblog.config.annotation;

import com.devon.techblog.config.ImageStorageMockConfig;
import com.devon.techblog.config.JpaAuditingTestConfig;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * 비HTTP 플로우(서비스, 배치, 메시지 리스너 등)를 위한 통합 테스트 메타 어노테이션입니다.
 * - @ActiveProfiles("test") : 테스트 프로필 활성화
 * - @SpringBootTest(webEnvironment = NONE) : 전체 컨텍스트를 로드하되 웹 서버/MockMvc는 구동하지 않음
 * - @Import(JpaAuditingTestConfig, ImageStorageMockConfig) :
 *   JPA Auditing, 이미지 스토리지에 대한 테스트 전용 설정 주입
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ActiveProfiles("test")
@Import({JpaAuditingTestConfig.class, ImageStorageMockConfig.class})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Tag("service-integration")
public @interface ServiceIntegrationTest {
}

