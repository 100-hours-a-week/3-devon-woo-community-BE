package com.devon.techblog.application.member;

import com.devon.techblog.application.member.dto.request.MemberUpdateRequest;

public final class MemberRequestFixture {

    public static final String DEFAULT_NEW_NICKNAME = "newNick";
    public static final String DEFAULT_NEW_PROFILE_IMAGE = "https://example.com/new.png";

    private MemberRequestFixture() {}

    public static MemberUpdateRequest updateRequest() {
        return new MemberUpdateRequest(
                DEFAULT_NEW_NICKNAME,
                DEFAULT_NEW_PROFILE_IMAGE,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    public static MemberUpdateRequest updateRequest(String nickname, String profileImageUrl) {
        return new MemberUpdateRequest(
                nickname,
                profileImageUrl,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }
}
