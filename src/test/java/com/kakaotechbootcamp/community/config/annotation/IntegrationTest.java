package com.kakaotechbootcamp.community.config.annotation;

import com.kakaotechbootcamp.community.config.TestConfig;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * 전체 애플리케이션 컨텍스트(SpringBootTest)를 띄워 Controller → Service → Repository →
 * DB 흐름과 Security/Infrastructure를 포함한 실제 통합 시나리오를 검증하기 위한 테스트 어노테이션.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ActiveProfiles("test")
@Import(TestConfig.class)
@SpringBootTest
@Tag("integration")
public @interface IntegrationTest {
}
