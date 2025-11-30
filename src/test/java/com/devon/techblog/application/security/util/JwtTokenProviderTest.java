package com.devon.techblog.application.security.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.devon.techblog.application.security.config.JwtProperties;
import com.devon.techblog.application.security.constants.JwtConstants;
import com.devon.techblog.config.annotation.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UnitTest
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("0123456789abcdef0123456789abcdef");
        properties.setAccessTokenExpiration(60_000L);
        properties.setRefreshTokenExpiration(120_000L);
        jwtTokenProvider = new JwtTokenProvider(properties);
    }

    @Test
    @DisplayName("액세스 토큰을 발급하면 type과 role, uid를 읽을 수 있다")
    void generateAccessToken_populatesClaims() {
        String token = jwtTokenProvider.generateAccessToken(1L, "USER");

        assertThat(jwtTokenProvider.isAccessToken(token)).isTrue();
        assertThat(jwtTokenProvider.isRefreshToken(token)).isFalse();
        assertThat(jwtTokenProvider.getTokenType(token)).isEqualTo(JwtConstants.TOKEN_TYPE_ACCESS);
        assertThat(jwtTokenProvider.getRoleFromToken(token)).isEqualTo("USER");
        assertThat(jwtTokenProvider.getUidFromToken(token)).isEqualTo(1L);
        assertThat(jwtTokenProvider.isTokenExpired(token)).isFalse();
    }

    @Test
    @DisplayName("리프레시 토큰을 발급하면 타입과 만료 시간을 확인할 수 있다")
    void generateRefreshToken_producesRefreshToken() {
        String token = jwtTokenProvider.generateRefreshToken(2L);

        assertThat(jwtTokenProvider.isRefreshToken(token)).isTrue();
        assertThat(jwtTokenProvider.getTokenType(token)).isEqualTo(JwtConstants.TOKEN_TYPE_REFRESH);
        assertThat(jwtTokenProvider.getUidFromToken(token)).isEqualTo(2L);
        assertThat(jwtTokenProvider.getExpiresIn(token)).isGreaterThan(0L);
    }

    @Test
    @DisplayName("유효하지 않은 토큰은 만료된 것으로 처리한다")
    void isTokenExpired_invalidToken() {
        assertThat(jwtTokenProvider.isTokenExpired("invalid")).isTrue();
    }

    @Test
    @DisplayName("만료된 토큰은 true를 반환한다")
    void isTokenExpired_returnsTrueAfterExpiration() throws InterruptedException {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("abcdefghijklmnopabcdefghijklmnop");
        properties.setAccessTokenExpiration(10L);
        properties.setRefreshTokenExpiration(10L);
        JwtTokenProvider shortLivedProvider = new JwtTokenProvider(properties);

        String token = shortLivedProvider.generateAccessToken(3L, "USER");
        Thread.sleep(20L);

        assertThat(shortLivedProvider.isTokenExpired(token)).isTrue();
    }
}
