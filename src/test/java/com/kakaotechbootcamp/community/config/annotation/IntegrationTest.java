package com.kakaotechbootcamp.community.config.annotation;

import com.kakaotechbootcamp.community.config.ImageStorageMockConfig;
import com.kakaotechbootcamp.community.config.JpaAuditingTestConfig;
import com.kakaotechbootcamp.community.config.TestSecurityConfig;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ActiveProfiles("test")
@Import({TestSecurityConfig.class, JpaAuditingTestConfig.class, ImageStorageMockConfig.class})
@SpringBootTest
@AutoConfigureMockMvc
@Tag("integration")
public @interface IntegrationTest {
}
