package com.devon.techblog.application.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.devon.techblog.application.member.MemberRequestFixture;
import com.devon.techblog.application.member.dto.request.MemberUpdateRequest;
import com.devon.techblog.application.member.dto.request.PasswordUpdateRequest;
import com.devon.techblog.application.member.dto.response.MemberDetailsResponse;
import com.devon.techblog.application.member.dto.response.MemberUpdateResponse;
import com.devon.techblog.application.member.validator.MemberValidator;
import com.devon.techblog.common.exception.CustomException;
import com.devon.techblog.config.annotation.UnitTest;
import com.devon.techblog.domain.member.MemberFixture;
import com.devon.techblog.domain.member.entity.Member;
import com.devon.techblog.domain.member.entity.MemberStatus;
import com.devon.techblog.domain.member.repository.MemberRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@UnitTest
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MemberValidator memberValidator;

    @InjectMocks
    private MemberService memberService;

    private Member member;

    @BeforeEach
    void setUp() {
        member = MemberFixture.createWithId(1L, "user@test.com", "password1234", "tester");
        member.updateProfileImage("https://example.com/profile.png");
    }

    @Test
    @DisplayName("회원 프로필을 조회할 수 있다")
    void getMemberProfile_success() {
        given(memberRepository.findByIdAndStatus(1L, MemberStatus.ACTIVE)).willReturn(Optional.of(member));

        MemberDetailsResponse response = memberService.getMemberProfile(1L);

        assertThat(response.memberId()).isEqualTo(1L);
        assertThat(response.nickname()).isEqualTo("tester");
    }

    @Test
    @DisplayName("존재하지 않는 회원 조회 시 예외가 발생한다")
    void getMemberProfile_notFound() {
        given(memberRepository.findByIdAndStatus(1L, MemberStatus.ACTIVE)).willReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.getMemberProfile(1L))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("회원 정보를 수정하면 닉네임 검증 후 저장된다")
    void updateMember_success() {
        MemberUpdateRequest request = MemberRequestFixture.updateRequest();
        given(memberRepository.findByIdAndStatus(1L, MemberStatus.ACTIVE)).willReturn(Optional.of(member));
        given(memberRepository.save(member)).willReturn(member);

        MemberUpdateResponse response = memberService.updateMember(1L, request);

        assertThat(response.nickname()).isEqualTo("newNick");
        assertThat(response.profileImage()).isEqualTo("https://example.com/new.png");
    }

    @Test
    @DisplayName("비밀번호 변경 시 검증 후 저장된다")
    void updatePassword_success() {
        PasswordUpdateRequest request = new PasswordUpdateRequest("password1234", "newPassword123");
        given(memberRepository.findByIdAndStatus(1L, MemberStatus.ACTIVE)).willReturn(Optional.of(member));
        given(memberRepository.save(member)).willReturn(member);

        memberService.updatePassword(1L, request);

        assertThat(member.getPassword()).isEqualTo("newPassword123");
    }

    @Test
    @DisplayName("회원 탈퇴 시 상태가 변경되고 저장된다")
    void deleteMember_success() {
        given(memberRepository.findByIdAndStatus(1L, MemberStatus.ACTIVE)).willReturn(Optional.of(member));

        memberService.deleteMember(1L);

        assertThat(member.getStatus()).isEqualTo(MemberStatus.WITHDRAWN);
    }
}
