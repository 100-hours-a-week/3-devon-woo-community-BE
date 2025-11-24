package com.kakaotechbootcamp.community.domain.post.policy;

import static org.assertj.core.api.Assertions.assertThat;

import com.kakaotechbootcamp.community.application.post.dto.ViewContext;
import com.kakaotechbootcamp.community.config.annotation.UnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UnitTest
class ViewCountPolicyTest {

    private final ViewCountPolicy viewCountPolicy = new ViewCountPolicy();

    @Test
    @DisplayName("조회수 증가 정책은 항상 true를 반환한다")
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
