package com.kakaotechbootcamp.community.domain.member.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.kakaotechbootcamp.community.config.IntegrationTest;
import com.kakaotechbootcamp.community.domain.member.entity.Member;
import com.kakaotechbootcamp.community.domain.member.entity.MemberStatus;
import jakarta.transaction.Transactional;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
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
        Member member = memberRepository.save(Member.create("member@test.com", "password", "tester"));

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
        Member active = memberRepository.save(Member.create("active@test.com", "password", "active"));
        Member inactive = Member.create("inactive@test.com", "password", "inactive");
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
}
