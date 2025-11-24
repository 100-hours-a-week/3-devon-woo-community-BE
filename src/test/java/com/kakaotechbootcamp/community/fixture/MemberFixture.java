package com.kakaotechbootcamp.community.fixture;

import com.kakaotechbootcamp.community.domain.member.entity.Member;
import org.springframework.test.util.ReflectionTestUtils;

public class MemberFixture {

    public static Member createMember() {
        return createMember(1L, "user@test.com", "password123", "tester");
    }

    public static Member createMember(Long id) {
        return createMember(id, "user@test.com", "password123", "tester");
    }

    public static Member createMember(Long id, String email, String password, String nickname) {
        Member member = Member.create(email, password, nickname);
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    public static Member createMemberWithEmail(String email) {
        return createMember(1L, email, "password123", "tester");
    }

    public static Member createMemberWithNickname(String nickname) {
        return createMember(1L, "user@test.com", "password123", nickname);
    }
}
