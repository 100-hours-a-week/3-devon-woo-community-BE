package com.devon.techblog.domain.member.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.devon.techblog.application.member.dto.SocialLinks;
import com.devon.techblog.config.annotation.RepositoryJpaTest;
import com.devon.techblog.domain.member.MemberFixture;
import com.devon.techblog.domain.member.entity.Member;
import com.devon.techblog.domain.member.entity.MemberStatus;
import jakarta.transaction.Transactional;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@RepositoryJpaTest
@Transactional
class MemberRepositoryTest {

    @Autowired
    private MemberRepository memberRepository;

    @AfterEach
    void tearDown() {
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("이메일로 회원을 조회하고 존재 여부를 확인할 수 있다")
    void findByEmailAndExists() {
        Member member = memberRepository.save(MemberFixture.create("member@test.com", "password", "tester"));

        assertThat(memberRepository.findByEmail("member@test.com"))
                .isPresent()
                .get()
                .extracting(Member::getId)
                .isEqualTo(member.getId());

        assertThat(memberRepository.existsByEmail("member@test.com")).isTrue();
        assertThat(memberRepository.existsByEmail("unknown@test.com")).isFalse();
    }

    @Test
    @DisplayName("상태별 조회는 ACTIVE 회원만 반환한다")
    void findByStatus_returnsActiveMembersOnly() {
        Member active = memberRepository.save(MemberFixture.create("active@test.com", "password", "active"));
        Member inactive = MemberFixture.create("inactive@test.com", "password", "inactive");
        inactive.deactivate();
        memberRepository.save(inactive);

        List<Member> actives = memberRepository.findByStatus(MemberStatus.ACTIVE);

        assertThat(actives)
                .extracting(Member::getEmail)
                .containsExactly(active.getEmail());

        assertThat(memberRepository.findByStatus(MemberStatus.INACTIVE))
                .extracting(Member::getEmail)
                .containsExactly(inactive.getEmail());
    }

    @Test
    @DisplayName("새 필드들이 정상적으로 저장되고 조회된다")
    void saveAndFindWithNewFields() {
        Member member = MemberFixture.create("user@test.com", "password", "tester");

        List<String> stack = Arrays.asList("Java", "Spring");
        List<String> interests = Arrays.asList("AI", "Cloud");
        SocialLinks links = new SocialLinks(
                "https://github.com/user",
                "https://user.com",
                "https://linkedin.com/in/user",
                null
        );

        member.updateProfile("@user", "Backend Dev", "Tech Corp", "Seoul", stack, interests, links);

        Member saved = memberRepository.save(member);
        Member found = memberRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getHandle()).isEqualTo("@user");
        assertThat(found.getBio()).isEqualTo("Backend Dev");
        assertThat(found.getCompany()).isEqualTo("Tech Corp");
        assertThat(found.getLocation()).isEqualTo("Seoul");
        assertThat(found.getPrimaryStack()).containsExactly("Java", "Spring");
        assertThat(found.getInterests()).containsExactly("AI", "Cloud");
        assertThat(found.getSocialLinks().github()).isEqualTo("https://github.com/user");
        assertThat(found.getSocialLinks().website()).isEqualTo("https://user.com");
        assertThat(found.getSocialLinks().linkedin()).isEqualTo("https://linkedin.com/in/user");
        assertThat(found.getSocialLinks().notion()).isNull();
    }

    @Test
    @DisplayName("빈 리스트와 null 필드가 정상적으로 저장되고 조회된다")
    void saveAndFindWithEmptyAndNullFields() {
        Member member = MemberFixture.create("user@test.com", "password", "tester");
        Member saved = memberRepository.save(member);
        Member found = memberRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getHandle()).isNull();
        assertThat(found.getBio()).isNull();
        assertThat(found.getCompany()).isNull();
        assertThat(found.getLocation()).isNull();
        assertThat(found.getPrimaryStack()).isEmpty();
        assertThat(found.getInterests()).isEmpty();
        assertThat(found.getSocialLinks()).isNotNull();
    }

    @Test
    @DisplayName("필드 수정 후 재조회 시 변경된 값이 반영된다")
    void updateAndReload() {
        Member member = memberRepository.save(MemberFixture.create("user@test.com", "password", "tester"));

        member.updateHandle("@newhandle");
        member.updateCompany("New Company");
        member.updatePrimaryStack(Arrays.asList("Kotlin", "Spring Boot"));

        memberRepository.save(member);
        memberRepository.flush();

        Member reloaded = memberRepository.findById(member.getId()).orElseThrow();

        assertThat(reloaded.getHandle()).isEqualTo("@newhandle");
        assertThat(reloaded.getCompany()).isEqualTo("New Company");
        assertThat(reloaded.getPrimaryStack()).containsExactly("Kotlin", "Spring Boot");
    }
}
