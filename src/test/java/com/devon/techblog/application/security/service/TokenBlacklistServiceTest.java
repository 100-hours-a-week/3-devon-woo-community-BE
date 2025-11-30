package com.devon.techblog.application.security.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.devon.techblog.application.security.util.JwtTokenProvider;
import com.devon.techblog.config.annotation.UnitTest;
import com.devon.techblog.infra.redis.adapter.RedisService;
import java.time.Duration;
import java.util.Optional;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@UnitTest
class TokenBlacklistServiceTest {

    private static final String TOKEN = "refresh-token";
    private static final long TTL_MILLIS = 120_000L;

    @Mock
    private RedisService redisService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private TokenBlacklistService tokenBlacklistService;

    @Test
    @DisplayName("토큰을 블랙리스트에 등록하면 sha256 해시 키와 TTL로 Redis에 저장한다")
    void addToBlacklist_savesHashedKeyAndTtl() {
        given(jwtTokenProvider.getExpiresIn(TOKEN)).willReturn(TTL_MILLIS);

        tokenBlacklistService.addToBlacklist(TOKEN);

        String expectedKey = "blacklist:refresh-token:" + DigestUtils.sha256Hex(TOKEN);
        verify(redisService).save(eq(expectedKey), eq("0"), eq(Duration.ofMillis(TTL_MILLIS)));
    }

    @Test
    @DisplayName("블랙리스트에 등록된 토큰은 존재함을 반환한다")
    void isBlacklisted_returnsTrueWhenPresent() {
        String expectedKey = "blacklist:refresh-token:" + DigestUtils.sha256Hex(TOKEN);
        given(redisService.find(expectedKey)).willReturn(Optional.of("0"));

        boolean actual = tokenBlacklistService.isBlacklisted(TOKEN);

        assertThat(actual).isTrue();
    }

    @Test
    @DisplayName("블랙리스트에 없는 토큰은 false를 반환한다")
    void isBlacklisted_returnsFalseWhenAbsent() {
        String expectedKey = "blacklist:refresh-token:" + DigestUtils.sha256Hex(TOKEN);
        given(redisService.find(expectedKey)).willReturn(Optional.empty());

        boolean actual = tokenBlacklistService.isBlacklisted(TOKEN);

        assertThat(actual).isFalse();
    }
}
