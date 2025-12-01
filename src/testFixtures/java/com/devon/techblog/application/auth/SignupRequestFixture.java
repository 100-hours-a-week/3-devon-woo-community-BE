package com.devon.techblog.application.auth;

import com.devon.techblog.application.member.dto.request.SignupRequest;

public class SignupRequestFixture {

    public static final String DEFAULT_EMAIL = "user@test.com";
    public static final String DEFAULT_PASSWORD = "password1234";
    public static final String DEFAULT_NICKNAME = "tester";
    public static final String DEFAULT_PROFILE_IMAGE = "https://example.com/profile.png";

    private SignupRequestFixture() {
    }

    public static SignupRequest createRequest() {
        return new SignupRequest(SignupRequestFixture.DEFAULT_EMAIL, SignupRequestFixture.DEFAULT_PASSWORD,
                SignupRequestFixture.DEFAULT_NICKNAME, SignupRequestFixture.DEFAULT_PROFILE_IMAGE);
    }


    public static SignupRequest createRequest(String email, String password, String nickname, String profileUrl) {
        return new SignupRequest(email, password, nickname, profileUrl);
    }
}