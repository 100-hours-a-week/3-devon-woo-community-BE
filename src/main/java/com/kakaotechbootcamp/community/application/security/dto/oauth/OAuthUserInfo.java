package com.kakaotechbootcamp.community.application.security.dto.oauth;

public interface OAuthUserInfo {
    String getId();
    String getProvider();
    String getEmail();
    String getName();
}