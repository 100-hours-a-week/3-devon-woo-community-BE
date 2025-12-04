package com.devon.techblog.domain.member.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.devon.techblog.application.member.dto.SocialLinks;
import com.devon.techblog.config.annotation.UnitTest;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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

    @Test
    @DisplayName("create 시 새 필드들이 기본값으로 초기화된다")
    void create_initializesNewFieldsWithDefaults() {
        Member member = Member.create("user@test.com", "password123", "tester");

        assertThat(member.getPrimaryStack()).isEmpty();
        assertThat(member.getInterests()).isEmpty();
        assertThat(member.getSocialLinks()).isNotNull();
        assertThat(member.getHandle()).isNull();
        assertThat(member.getBio()).isNull();
        assertThat(member.getCompany()).isNull();
        assertThat(member.getLocation()).isNull();
    }

    @Test
    @DisplayName("핸들은 50자를 초과할 수 없다")
    void updateHandle_lengthGuard() {
        Member member = Member.create("user@test.com", "password123", "tester");

        member.updateHandle("@devon");
        assertThat(member.getHandle()).isEqualTo("@devon");

        member.updateHandle(null);
        assertThat(member.getHandle()).isNull();

        assertThatThrownBy(() -> member.updateHandle("a".repeat(51)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("handle too long");
    }

    @Test
    @DisplayName("자기소개는 제한 없이 설정 가능하다")
    void updateBio_noRestrictions() {
        Member member = Member.create("user@test.com", "password123", "tester");

        String longBio = "안녕하세요. ".repeat(100);
        member.updateBio(longBio);
        assertThat(member.getBio()).isEqualTo(longBio);

        member.updateBio(null);
        assertThat(member.getBio()).isNull();
    }

    @Test
    @DisplayName("회사는 100자를 초과할 수 없다")
    void updateCompany_lengthGuard() {
        Member member = Member.create("user@test.com", "password123", "tester");

        member.updateCompany("Tech Company");
        assertThat(member.getCompany()).isEqualTo("Tech Company");

        assertThatThrownBy(() -> member.updateCompany("a".repeat(101)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("company too long");
    }

    @Test
    @DisplayName("위치는 100자를 초과할 수 없다")
    void updateLocation_lengthGuard() {
        Member member = Member.create("user@test.com", "password123", "tester");

        member.updateLocation("Seoul, Korea");
        assertThat(member.getLocation()).isEqualTo("Seoul, Korea");

        assertThatThrownBy(() -> member.updateLocation("a".repeat(101)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("location too long");
    }

    @Test
    @DisplayName("주요 기술 스택 목록을 설정할 수 있다")
    void updatePrimaryStack_setsList() {
        Member member = Member.create("user@test.com", "password123", "tester");

        List<String> stack = Arrays.asList("Java", "Spring", "JPA");
        member.updatePrimaryStack(stack);
        assertThat(member.getPrimaryStack()).containsExactly("Java", "Spring", "JPA");

        member.updatePrimaryStack(null);
        assertThat(member.getPrimaryStack()).isEmpty();

        member.updatePrimaryStack(Collections.emptyList());
        assertThat(member.getPrimaryStack()).isEmpty();
    }

    @Test
    @DisplayName("관심사 목록을 설정할 수 있다")
    void updateInterests_setsList() {
        Member member = Member.create("user@test.com", "password123", "tester");

        List<String> interests = Arrays.asList("AI", "Cloud", "DevOps");
        member.updateInterests(interests);
        assertThat(member.getInterests()).containsExactly("AI", "Cloud", "DevOps");

        member.updateInterests(null);
        assertThat(member.getInterests()).isEmpty();
    }

    @Test
    @DisplayName("소셜 링크를 설정할 수 있다")
    void updateSocialLinks_setsLinks() {
        Member member = Member.create("user@test.com", "password123", "tester");

        SocialLinks links = new SocialLinks(
                "https://github.com/devon",
                "https://devon.com",
                "https://linkedin.com/in/devon",
                "https://notion.so/devon"
        );
        member.updateSocialLinks(links);
        assertThat(member.getSocialLinks()).isEqualTo(links);

        member.updateSocialLinks(null);
        assertThat(member.getSocialLinks()).isNotNull();
    }

    @Test
    @DisplayName("updateProfile로 모든 프로필 정보를 한번에 수정할 수 있다")
    void updateProfile_updatesAllFields() {
        Member member = Member.create("user@test.com", "password123", "tester");

        List<String> stack = Arrays.asList("Kotlin", "Spring Boot");
        List<String> interests = Arrays.asList("Architecture", "DDD");
        SocialLinks links = new SocialLinks("https://github.com/devon", null, null, null);

        member.updateProfile(
                "@devon_new",
                "Backend Developer",
                "New Company",
                "Busan",
                stack,
                interests,
                links
        );

        assertThat(member.getHandle()).isEqualTo("@devon_new");
        assertThat(member.getBio()).isEqualTo("Backend Developer");
        assertThat(member.getCompany()).isEqualTo("New Company");
        assertThat(member.getLocation()).isEqualTo("Busan");
        assertThat(member.getPrimaryStack()).containsExactly("Kotlin", "Spring Boot");
        assertThat(member.getInterests()).containsExactly("Architecture", "DDD");
        assertThat(member.getSocialLinks()).isEqualTo(links);
    }
}
