package com.kakaotechbootcamp.community.domain.member.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kakaotechbootcamp.community.config.UnitTest;
import com.kakaotechbootcamp.community.domain.member.entity.MemberRole;
import com.kakaotechbootcamp.community.domain.member.entity.MemberStatus;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UnitTest
class MemberTest {

    @Test
    @DisplayName("create 시 기본 상태와 역할이 설정된다")
    void create_setsDefaultStatusAndRole() {
        Member member = Member.create("user@test.com", "password123", "tester");

        assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
        assertThat(member.getRole()).isEqualTo(MemberRole.USER);
        assertThat(member.getEmail()).isEqualTo("user@test.com");
        assertThat(member.getNickname()).isEqualTo("tester");
    }

    @Test
    @DisplayName("create 시 필수값이 없으면 예외가 발생한다")
    void create_requiresMandatoryFields() {
        assertThatThrownBy(() -> Member.create("", "pass", "nick"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> Member.create("user@test.com", "", "nick"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> Member.create("user@test.com", "pass", ""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("닉네임은 10자를 초과할 수 없다")
    void changeNickname_lengthGuard() {
        Member member = Member.create("user@test.com", "password123", "tester");

        assertThatThrownBy(() -> member.changeNickname("01234567890"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nickname too long");

        member.changeNickname("최대열글자");
        assertThat(member.getNickname()).isEqualTo("최대열글자");
    }

    @Test
    @DisplayName("프로필 이미지는 null 허용, 500자 초과 불가")
    void updateProfileImage_validatesLength() {
        Member member = Member.create("user@test.com", "password123", "tester");

        member.updateProfileImage(null);
        assertThat(member.getProfileImageUrl()).isNull();

        String maxLengthUrl = "https://example.com/" + "a".repeat(470);
        member.updateProfileImage(maxLengthUrl);
        assertThat(member.getProfileImageUrl()).isEqualTo(maxLengthUrl);

        assertThatThrownBy(() -> member.updateProfileImage("https://example.com/" + "a".repeat(482)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("url too long");
    }

    @Test
    @DisplayName("비밀번호 변경은 빈 값일 수 없다")
    void changePassword_requiresValue() {
        Member member = Member.create("user@test.com", "password123", "tester");

        member.changePassword("newPassword");
        assertThat(member.getPassword()).isEqualTo("newPassword");

        assertThatThrownBy(() -> member.changePassword(" "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("로그인 성공 시각이 갱신되고 상태 전환이 가능하다")
    void loginAndStatusTransitions() {
        Member member = Member.create("user@test.com", "password123", "tester");

        member.loginSuccess();
        Instant firstLogin = member.getLastLoginAt();
        assertThat(firstLogin).isNotNull();

        member.deactivate();
        assertThat(member.getStatus()).isEqualTo(MemberStatus.INACTIVE);
        assertThat(member.isActive()).isFalse();

        member.withdraw();
        assertThat(member.getStatus()).isEqualTo(MemberStatus.WITHDRAWN);
    }
}
