package com.devon.techblog.domain.post.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devon.techblog.application.post.dto.ViewContext;
import com.devon.techblog.config.annotation.UnitTest;
import com.devon.techblog.infra.redis.adapter.RedisService;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@UnitTest
class ViewCountPolicyTest {

    @Mock
    private RedisService redisService;

    @InjectMocks
    private ViewCountPolicy viewCountPolicy;

    @Test
    void shouldCount_whenRedisKeyMissing_returnsTrueAndRecordsKey() {
        ViewContext context = ViewContext.builder()
                .memberId(null)
                .ipAddress("127.0.0.1")
                .userAgent("test-agent")
                .build();

        when(redisService.find(anyString())).thenReturn(Optional.empty());

        boolean result = viewCountPolicy.shouldCount(101L, context);

        assertThat(result).isTrue();

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(redisService).find(keyCaptor.capture());
        String generatedKey = keyCaptor.getValue();

        assertThat(generatedKey).startsWith("view-count:101:ip:127.0.0.1");
        assertThat(generatedKey).contains(":ua:" + context.getUserAgent().hashCode());

        verify(redisService).save(eq(generatedKey), eq("1"), eq(Duration.ofMinutes(10)));
    }

    @Test
    void shouldCount_whenRedisKeyPresent_returnsFalseWithoutSaving() {
        ViewContext context = ViewContext.builder()
                .memberId(42L)
                .ipAddress("9.9.9.9")
                .userAgent("different-agent")
                .build();

        String expectedKey = "view-count:202:member:42";
        when(redisService.find(expectedKey)).thenReturn(Optional.of("1"));

        boolean result = viewCountPolicy.shouldCount(202L, context);

        assertThat(result).isFalse();

        verify(redisService).find(expectedKey);
        verify(redisService, never()).save(anyString(), anyString(), isA(Duration.class));
    }
}
