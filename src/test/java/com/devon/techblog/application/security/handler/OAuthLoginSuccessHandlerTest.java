package com.devon.techblog.application.security.handler;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.devon.techblog.application.security.dto.user.CustomOAuthUserDetails;
import com.devon.techblog.application.security.repository.HttpCookieOAuth2AuthorizationRequestRepository;
import com.devon.techblog.application.security.util.CookieProvider;
import com.devon.techblog.application.security.util.JwtTokenProvider;
import com.devon.techblog.config.annotation.UnitTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.RedirectStrategy;

@UnitTest
class OAuthLoginSuccessHandlerTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private CookieProvider cookieProvider;

    @Mock
    private HttpCookieOAuth2AuthorizationRequestRepository authorizationRequestRepository;

    @Mock
    private RedirectStrategy redirectStrategy;

    private OAuthLoginSuccessHandler handler;

    @BeforeEach
    void setUp() {
        handler = new OAuthLoginSuccessHandler(jwtTokenProvider, cookieProvider, authorizationRequestRepository);
        handler.setRedirectStrategy(redirectStrategy);
    }

    @Test
    @DisplayName("Redirect URI 쿠키가 있으면 해당 URI로 콜백하고 리프레시 토큰 쿠키를 추가한다")
    void onAuthenticationSuccess_redirectsToCookieUri() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("redirect_uri", "http://frontend.dev"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        Authentication authentication = mock(Authentication.class);
        CustomOAuthUserDetails principal = new CustomOAuthUserDetails(10L, "USER");

        given(authentication.getPrincipal()).willReturn(principal);
        given(jwtTokenProvider.generateRefreshToken(10L)).willReturn("refresh-token");

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(jwtTokenProvider).generateRefreshToken(10L);
        verify(cookieProvider).addRefreshTokenCookie(response, "refresh-token");
        verify(authorizationRequestRepository).removeAuthorizationRequestCookies(response);
        verify(redirectStrategy).sendRedirect(request, response, "http://frontend.dev/oauth/callback");
    }

}
