package com.devon.techblog.application.security.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.devon.techblog.application.security.service.TokenBlacklistService;
import com.devon.techblog.application.security.util.CookieProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.devon.techblog.config.annotation.UnitTest;
import java.io.IOException;
import java.util.Optional;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@UnitTest
class LogoutHandlerTest {

    @Mock
    private CookieProvider cookieProvider;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    private LogoutHandler logoutHandler;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        logoutHandler = new LogoutHandler(objectMapper, cookieProvider, tokenBlacklistService);
    }

    @Test
    @DisplayName("리프레시 토큰이 있으면 블랙리스트에 등록하고 쿠키를 삭제한다")
    void onLogout_withRefreshToken() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        given(cookieProvider.getRefreshTokenFromCookie(request)).willReturn(Optional.of("refresh"));

        logoutHandler.onLogout(request, response);

        verify(tokenBlacklistService).addToBlacklist("refresh");
        verify(cookieProvider).deleteRefreshTokenCookie(response);
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
        assertThat(response.getContentAsString()).contains("로그아웃되었습니다.");
    }

    @Test
    @DisplayName("리프레시 토큰이 없으면 블랙리스트 등록은 하지 않는다")
    void onLogout_withoutRefreshToken() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        given(cookieProvider.getRefreshTokenFromCookie(request)).willReturn(Optional.empty());

        logoutHandler.onLogout(request, response);

        verify(tokenBlacklistService, never()).addToBlacklist("refresh");
        verify(cookieProvider).deleteRefreshTokenCookie(response);
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
    }
}
