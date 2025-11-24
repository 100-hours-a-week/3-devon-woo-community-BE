package com.kakaotechbootcamp.community.config.annotation;

import com.kakaotechbootcamp.community.config.ImageStorageMockConfig;
import com.kakaotechbootcamp.community.config.JpaAuditingTestConfig;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ActiveProfiles("test")
@Import({JpaAuditingTestConfig.class, ImageStorageMockConfig.class})
@SpringBootTest
@Tag("integration")
public @interface IntegrationTest {
}
