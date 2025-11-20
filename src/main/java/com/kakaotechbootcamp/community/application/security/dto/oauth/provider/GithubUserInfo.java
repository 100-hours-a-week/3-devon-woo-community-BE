package com.kakaotechbootcamp.community.application.security.dto.oauth.provider;

import com.kakaotechbootcamp.community.application.security.dto.oauth.OAuthUserInfo;
import java.util.Map;

public class GithubUserInfo implements OAuthUserInfo {
    private final Map<String, Object> attributes;

    public GithubUserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String getProvider() {
        return "github";
    }

    @Override
    public String getId() {
        return String.valueOf(attributes.get("id"));
    }


    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }

    @Override
    public String getName() {
        return (String) attributes.get("name");
    }
}
