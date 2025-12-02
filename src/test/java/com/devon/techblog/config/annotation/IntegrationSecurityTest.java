package com.devon.techblog.config.annotation;

import com.devon.techblog.config.ImageStorageMockConfig;
import com.devon.techblog.config.JpaAuditingTestConfig;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * 실제 Security 설정을 사용하는 통합 테스트를 위한 메타 어노테이션입니다.
 * 기존 {@link IntegrationTest} 에서 TestSecurityConfig만 제외하고,
 * Auditing/Image Mock 구성은 동일하게 적용합니다.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ActiveProfiles("test")
@Import({JpaAuditingTestConfig.class, ImageStorageMockConfig.class})
@SpringBootTest
@AutoConfigureMockMvc
@Tag("integration-security")
public @interface IntegrationSecurityTest {
}
