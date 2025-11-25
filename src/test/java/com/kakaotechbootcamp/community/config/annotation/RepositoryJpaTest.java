package com.kakaotechbootcamp.community.config.annotation;

import com.kakaotechbootcamp.community.config.JpaAuditingTestConfig;
import com.kakaotechbootcamp.community.domain.config.QueryDslConfig;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * JPA 리포지토리 슬라이스 테스트를 위한 메타 어노테이션입니다.
 * - @DataJpaTest : JPA 관련 빈만 로드하는 슬라이스 테스트
 * - @ActiveProfiles(\"test\") : 테스트 프로필 활성화
 * - @Import(QueryDslConfig, JpaAuditingTestConfig) : QueryDSL 설정과 테스트용 JPA Auditing 설정 주입
 * - @AutoConfigureTestDatabase(replace = NONE) : 실제 설정된 DB(H2, Testcontainers 등)를 그대로 사용
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ActiveProfiles("test")
@Import({QueryDslConfig.class, JpaAuditingTestConfig.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
@Tag("repository")
public @interface RepositoryJpaTest {
}
