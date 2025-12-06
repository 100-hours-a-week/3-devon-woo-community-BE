package com.devon.techblog.application.security.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.devon.techblog.application.security.dto.user.CustomUserDetails;
import com.devon.techblog.application.security.util.CookieProvider;
import com.devon.techblog.application.security.util.JwtTokenProvider;
import com.devon.techblog.config.annotation.UnitTest;
import com.devon.techblog.domain.member.entity.MemberRole;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;

@UnitTest
class LoginSuccessHandlerTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private CookieProvider cookieProvider;

    private LoginSuccessHandler loginSuccessHandler;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        loginSuccessHandler = new LoginSuccessHandler(objectMapper, jwtTokenProvider, cookieProvider);
    }

    @Test
    @DisplayName("로그인 성공 시 액세스/리프레시 토큰을 발급하고 응답 본문에 JSON을 작성한다")
    void onAuthenticationSuccess_writesTokenResponse() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        Authentication authentication = mock(Authentication.class);
        CustomUserDetails principal = new CustomUserDetails(1L, "encoded", MemberRole.USER);

        given(authentication.getPrincipal()).willReturn(principal);
        given(authentication.getName()).willReturn("1");
        given(jwtTokenProvider.generateAccessToken(anyLong(), anyString())).willReturn("access-token");
        given(jwtTokenProvider.generateRefreshToken(1L)).willReturn("refresh-token");

        loginSuccessHandler.onAuthenticationSuccess(request, response, authentication);

        verify(jwtTokenProvider).generateAccessToken(1L, "USER");
        verify(jwtTokenProvider).generateRefreshToken(1L);
        verify(cookieProvider).addRefreshTokenCookie(response, "refresh-token");

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(body.get("success").asBoolean()).isTrue();
        assertThat(body.get("message").asText()).isEqualTo("로그인이 성공했습니다");
        assertThat(body.get("data").get("userId").asLong()).isEqualTo(1L);
        assertThat(body.get("data").get("accessToken").asText()).isEqualTo("access-token");
    }
}
