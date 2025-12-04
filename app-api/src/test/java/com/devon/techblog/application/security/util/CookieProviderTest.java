package com.devon.techblog.application.security.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.devon.techblog.application.security.config.properties.JwtProperties;
import com.devon.techblog.config.annotation.UnitTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@UnitTest
class CookieProviderTest {

    private CookieProvider cookieProvider;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setRefreshTokenExpiration(300_000L);
        cookieProvider = new CookieProvider(jwtProperties);
    }

    @Test
    @DisplayName("리프레시 토큰 쿠키를 추가하면 Set-Cookie 헤더에 이름과 경로, maxAge가 포함된다")
    void addRefreshTokenCookie_setsHeader() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        cookieProvider.addRefreshTokenCookie(response, "refresh123");

        String header = response.getHeader("Set-Cookie");
        assertThat(header).contains("refreshToken=refresh123");
        assertThat(header).contains("Max-Age=300"); // 300_000ms / 1000
        assertThat(header).contains("Path=/");
        assertThat(header).contains("HttpOnly");
    }

    @Test
    @DisplayName("쿠키에서 리프레시 토큰을 조회할 수 있다")
    void getRefreshTokenFromCookie_returnsValue() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("refreshToken", "cookie-token"));

        String actual = cookieProvider.getRefreshTokenFromCookie(request).orElseThrow();

        assertThat(actual).isEqualTo("cookie-token");
    }

    @Test
    @DisplayName("쿠키가 없는 경우 Optional.empty를 반환한다")
    void getRefreshTokenFromCookie_returnsEmpty() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertThat(cookieProvider.getRefreshTokenFromCookie(request)).isEmpty();
    }

    @Test
    @DisplayName("리프레시 토큰 쿠키 삭제 시 Max-Age=0이 설정된다")
    void deleteRefreshTokenCookie_setsMaxAgeZero() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        cookieProvider.deleteRefreshTokenCookie(response);

        String header = response.getHeader("Set-Cookie");
        assertThat(header).contains("refreshToken=");
        assertThat(header).contains("Max-Age=0");
    }
}
