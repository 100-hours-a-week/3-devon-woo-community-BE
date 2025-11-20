package com.kakaotechbootcamp.community.application.security.repository;

import com.kakaotechbootcamp.community.application.security.constants.CookieConstants;
import com.kakaotechbootcamp.community.application.security.util.CookieProvider;
import com.kakaotechbootcamp.community.application.security.util.RedirectUriValidator;
import com.kakaotechbootcamp.community.common.utils.SerializationUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;


@Component
@RequiredArgsConstructor
public class HttpCookieOAuth2AuthorizationRequestRepository implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    private final RedirectUriValidator redirectUriValidator;

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        return CookieProvider.getOAuth2AuthorizationRequestCookie(request)
                .map(value -> SerializationUtil.deserialize(value, OAuth2AuthorizationRequest.class))
                .orElse(null);
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest, HttpServletRequest request, HttpServletResponse response) {
        if (authorizationRequest == null) {
            removeAuthorizationRequestCookies(response);
            return;
        }
        CookieProvider.addOAuth2AuthorizationRequestCookie(response, SerializationUtil.serialize(authorizationRequest));

        String redirectUriAfterLogin = request.getParameter(CookieConstants.REDIRECT_URI_COOKIE_NAME);
        if (StringUtils.hasText(redirectUriAfterLogin) && redirectUriValidator.isValidRedirectUri(redirectUriAfterLogin)) {
            CookieProvider.addRedirectUriCookie(response, redirectUriAfterLogin);
        }
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request, HttpServletResponse response) {
        return loadAuthorizationRequest(request);
    }

    public void removeAuthorizationRequestCookies(HttpServletResponse response) {
        CookieProvider.deleteOAuth2AuthorizationRequestCookie(response);
        CookieProvider.deleteRedirectUriCookie(response);
    }
}