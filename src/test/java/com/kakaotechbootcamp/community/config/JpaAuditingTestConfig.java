package com.kakaotechbootcamp.community.config;

import java.time.Instant;
import java.util.Optional;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@TestConfiguration
@EnableJpaAuditing(dateTimeProviderRef = "testDateTimeProvider")
public class JpaAuditingTestConfig {

    private static final Instant FIXED_TEST_TIME = Instant.parse("2025-01-01T00:00:00Z");

    @Bean
    public TestClock testClock() {
        return new TestClock();
    }

    @Bean
    public DateTimeProvider testDateTimeProvider(TestClock testClock) {
        return () -> Optional.of(FIXED_TEST_TIME);
    }

    public static class TestClock {
        private Instant fixedInstant = FIXED_TEST_TIME;

        public Instant getFixedInstant() {
            return fixedInstant;
        }

        public void setFixedInstant(Instant instant) {
            this.fixedInstant = instant;
        }

        public void reset() {
            this.fixedInstant = FIXED_TEST_TIME;
        }
    }
}
