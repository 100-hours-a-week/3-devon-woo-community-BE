package com.kakaotechbootcamp.community.domain.member;

import com.kakaotechbootcamp.community.domain.member.entity.Member;
import org.springframework.test.util.ReflectionTestUtils;

public final class MemberFixture {

    public static final String DEFAULT_EMAIL = "user@test.com";
    public static final String DEFAULT_PASSWORD = "password123";
    public static final String DEFAULT_NICKNAME = "tester";

    private MemberFixture() {}

    public static Member create() {
        return Member.create(DEFAULT_EMAIL, DEFAULT_PASSWORD, DEFAULT_NICKNAME);
    }

    public static Member create(String email, String password, String nickname) {
        return Member.create(email, password, nickname);
    }

    public static Member createWithId(Long id) {
        Member member = create();
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    public static Member createWithId(Long id, String email, String password, String nickname) {
        Member member = create(email, password, nickname);
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }
}
