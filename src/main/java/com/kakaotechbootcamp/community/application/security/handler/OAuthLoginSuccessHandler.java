package com.kakaotechbootcamp.community.application.security.handler;

import com.kakaotechbootcamp.community.application.security.dto.user.CustomOAuthUserDetails;
import com.kakaotechbootcamp.community.application.security.repository.HttpCookieOAuth2AuthorizationRequestRepository;
import com.kakaotechbootcamp.community.application.security.util.CookieProvider;
import com.kakaotechbootcamp.community.application.security.util.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuthLoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final CookieProvider cookieProvider;
    private final HttpCookieOAuth2AuthorizationRequestRepository authorizationRequestRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        CustomOAuthUserDetails oAuth2User = (CustomOAuthUserDetails) authentication.getPrincipal();
        Long memberId = oAuth2User.getUid();

        String refreshToken = jwtTokenProvider.generateRefreshToken(memberId);
        cookieProvider.addRefreshTokenCookie(response, refreshToken);

        String redirectUri = determineTargetUrl(request);
        authorizationRequestRepository.removeAuthorizationRequestCookies(response);

        getRedirectStrategy().sendRedirect(request, response, redirectUri);

        log.info("OAuth2 로그인 성공 : id - {} / redirect url- {}", memberId, redirectUri);
    }

    private String determineTargetUrl(HttpServletRequest request) {
        return CookieProvider.getRedirectUriCookie(request)
                .orElse("http://localhost:3000");
    }
}
