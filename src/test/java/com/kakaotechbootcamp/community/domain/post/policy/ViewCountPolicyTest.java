package com.kakaotechbootcamp.community.domain.post.policy;

import static org.assertj.core.api.Assertions.assertThat;

import com.kakaotechbootcamp.community.application.post.dto.ViewContext;
import com.kakaotechbootcamp.community.config.annotation.UnitTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@UnitTest
class ViewCountPolicyTest {

    private final ViewCountPolicy viewCountPolicy = new ViewCountPolicy();

    @Test
    @Disabled("조회수 증가 - 개발전이라 테스트 비활성화")
    void shouldCount_returnsTrue() {
        ViewContext context = ViewContext.builder()
                .memberId(1L)
                .ipAddress("127.0.0.1")
                .userAgent("user-agent")
                .build();

        boolean result = viewCountPolicy.shouldCount(1L, context);

        assertThat(result).isTrue();
    }
}
