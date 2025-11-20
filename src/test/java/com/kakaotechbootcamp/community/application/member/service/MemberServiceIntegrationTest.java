package com.kakaotechbootcamp.community.application.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kakaotechbootcamp.community.application.member.dto.request.MemberUpdateRequest;
import com.kakaotechbootcamp.community.application.member.dto.request.PasswordUpdateRequest;
import com.kakaotechbootcamp.community.application.member.dto.response.MemberDetailsResponse;
import com.kakaotechbootcamp.community.application.member.dto.response.MemberUpdateResponse;
import com.kakaotechbootcamp.community.common.exception.CustomException;
import com.kakaotechbootcamp.community.common.exception.code.MemberErrorCode;
import com.kakaotechbootcamp.community.config.EnableSqlLogging;
import com.kakaotechbootcamp.community.config.IntegrationTest;
import com.kakaotechbootcamp.community.domain.member.entity.Member;
import com.kakaotechbootcamp.community.domain.member.entity.MemberStatus;
import com.kakaotechbootcamp.community.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

@IntegrationTest
@Sql(scripts = "/sql/member-service-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class MemberServiceIntegrationTest {

    @Autowired
    private MemberService memberService;

    @Autowired
    private MemberRepository memberRepository;

    private static final Long TEST_MEMBER1_ID = 1L;
    private static final Long TEST_MEMBER2_ID = 2L;

    @Test
    @EnableSqlLogging
    @DisplayName("회원 프로필 조회 성공")
    void getMemberProfile_Success() {
        // when
        MemberDetailsResponse response = memberService.getMemberProfile(TEST_MEMBER1_ID);

        // then
        assertThat(response).isNotNull();
        assertThat(response.memberId()).isEqualTo(TEST_MEMBER1_ID);
        assertThat(response.nickname()).isEqualTo("tester1");
        assertThat(response.profileImage()).isEqualTo("https://example.com/profile1.jpg");
    }

    @Test
    @EnableSqlLogging
    @DisplayName("회원 프로필 조회 실패 - 존재하지 않는 회원")
    void getMemberProfile_MemberNotFound_ThrowsException() {
        // given
        Long nonExistentMemberId = 99999L;

        // when & then
        assertThatThrownBy(() -> memberService.getMemberProfile(nonExistentMemberId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", MemberErrorCode.USER_NOT_FOUND);
    }

    @Test
    @EnableSqlLogging
    @DisplayName("회원 정보 수정 성공 - 닉네임만 변경")
    void updateMember_NicknameOnly_Success() {
        // given
        MemberUpdateRequest request = new MemberUpdateRequest(
                "new_nick",
                null
        );

        // when
        MemberUpdateResponse response = memberService.updateMember(TEST_MEMBER1_ID, request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.nickname()).isEqualTo("new_nick");

        // DB 검증
        Member updatedMember = memberRepository.findById(TEST_MEMBER1_ID).orElseThrow();
        assertThat(updatedMember.getNickname()).isEqualTo("new_nick");
    }

    @Test
    @EnableSqlLogging
    @DisplayName("회원 정보 수정 성공 - 프로필 이미지만 변경")
    void updateMember_ProfileImageOnly_Success() {
        // given
        MemberUpdateRequest request = new MemberUpdateRequest(
                null,
                "https://example.com/new-profile.jpg"
        );

        // when
        MemberUpdateResponse response = memberService.updateMember(TEST_MEMBER1_ID, request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.profileImage()).isEqualTo("https://example.com/new-profile.jpg");

        // DB 검증
        Member updatedMember = memberRepository.findById(TEST_MEMBER1_ID).orElseThrow();
        assertThat(updatedMember.getProfileImageUrl()).isEqualTo("https://example.com/new-profile.jpg");
    }

    @Test
    @EnableSqlLogging
    @DisplayName("회원 정보 수정 성공 - 닉네임과 프로필 이미지 모두 변경")
    void updateMember_BothFields_Success() {
        // given
        MemberUpdateRequest request = new MemberUpdateRequest(
                "up_nick",
                "https://example.com/updated-profile.jpg"
        );

        // when
        MemberUpdateResponse response = memberService.updateMember(TEST_MEMBER1_ID, request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.nickname()).isEqualTo("up_nick");
        assertThat(response.profileImage()).isEqualTo("https://example.com/updated-profile.jpg");

        // DB 검증
        Member updatedMember = memberRepository.findById(TEST_MEMBER1_ID).orElseThrow();
        assertThat(updatedMember.getNickname()).isEqualTo("up_nick");
        assertThat(updatedMember.getProfileImageUrl()).isEqualTo("https://example.com/updated-profile.jpg");
    }

    @Test
    @EnableSqlLogging
    @DisplayName("회원 정보 수정 실패 - 중복된 닉네임")
    void updateMember_DuplicateNickname_ThrowsException() {
        // given - TEST_MEMBER2_ID의 닉네임으로 변경 시도
        MemberUpdateRequest request = new MemberUpdateRequest(
                "tester2",  // TEST_MEMBER2_ID의 닉네임
                null
        );

        // when & then
        assertThatThrownBy(() -> memberService.updateMember(TEST_MEMBER1_ID, request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", MemberErrorCode.DUPLICATE_NICKNAME);
    }

    @Test
    @EnableSqlLogging
    @DisplayName("회원 정보 수정 실패 - 존재하지 않는 회원")
    void updateMember_MemberNotFound_ThrowsException() {
        // given
        Long nonExistentMemberId = 99999L;
        MemberUpdateRequest request = new MemberUpdateRequest(
                "new_nickname",
                null
        );

        // when & then
        assertThatThrownBy(() -> memberService.updateMember(nonExistentMemberId, request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", MemberErrorCode.USER_NOT_FOUND);
    }

    @Test
    @EnableSqlLogging
    @DisplayName("비밀번호 변경 성공")
    void updatePassword_Success() {
        // given
        PasswordUpdateRequest request = new PasswordUpdateRequest(
                "password123!",  // 현재 비밀번호
                "newPassword456!"  // 새 비밀번호
        );

        // when
        memberService.updatePassword(TEST_MEMBER1_ID, request);

        // then - DB 검증
        Member updatedMember = memberRepository.findById(TEST_MEMBER1_ID).orElseThrow();
        assertThat(updatedMember.getPassword()).isEqualTo("newPassword456!");
    }

    @Test
    @EnableSqlLogging
    @DisplayName("비밀번호 변경 실패 - 현재 비밀번호 불일치")
    void updatePassword_InvalidCurrentPassword_ThrowsException() {
        // given
        PasswordUpdateRequest request = new PasswordUpdateRequest(
                "wrongPassword",  // 잘못된 현재 비밀번호
                "newPassword456!"
        );

        // when & then
        assertThatThrownBy(() -> memberService.updatePassword(TEST_MEMBER1_ID, request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", MemberErrorCode.INVALID_CURRENT_PASSWORD);
    }

    @Test
    @EnableSqlLogging
    @DisplayName("비밀번호 변경 실패 - 새 비밀번호가 현재 비밀번호와 동일")
    void updatePassword_SameAsCurrentPassword_ThrowsException() {
        // given
        PasswordUpdateRequest request = new PasswordUpdateRequest(
                "password123!",  // 현재 비밀번호
                "password123!"   // 동일한 새 비밀번호
        );

        // when & then
        assertThatThrownBy(() -> memberService.updatePassword(TEST_MEMBER1_ID, request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", MemberErrorCode.SAME_AS_CURRENT_PASSWORD);
    }

    @Test
    @EnableSqlLogging
    @DisplayName("비밀번호 변경 실패 - 존재하지 않는 회원")
    void updatePassword_MemberNotFound_ThrowsException() {
        // given
        Long nonExistentMemberId = 99999L;
        PasswordUpdateRequest request = new PasswordUpdateRequest(
                "password123!",
                "newPassword456!"
        );

        // when & then
        assertThatThrownBy(() -> memberService.updatePassword(nonExistentMemberId, request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", MemberErrorCode.USER_NOT_FOUND);
    }

    @Test
    @EnableSqlLogging
    @DisplayName("회원 탈퇴 성공")
    void deleteMember_Success() {
        // when
        memberService.deleteMember(TEST_MEMBER1_ID);

        // then - DB 검증 (Soft delete 되어야 한다)
        Member deletedMember = memberRepository.findById(TEST_MEMBER1_ID).orElse(null);

        assertThat(deletedMember).isNotNull();
        assertThat(deletedMember.getStatus()).isEqualTo(MemberStatus.WITHDRAWN);
    }

    @Test
    @EnableSqlLogging
    @DisplayName("회원 탈퇴 실패 - 존재하지 않는 회원")
    void deleteMember_MemberNotFound_ThrowsException() {
        // given
        Long nonExistentMemberId = 99999L;

        // when & then
        assertThatThrownBy(() -> memberService.deleteMember(nonExistentMemberId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", MemberErrorCode.USER_NOT_FOUND);
    }

    @Test
    @EnableSqlLogging
    @DisplayName("회원 탈퇴 후 조회 실패")
    void deleteMember_ThenQuery_ThrowsException() {
        // given - 회원 탈퇴
        memberService.deleteMember(TEST_MEMBER1_ID);

        // when & then - 탈퇴한 회원 조회 시도
        assertThatThrownBy(() -> memberService.getMemberProfile(TEST_MEMBER1_ID))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", MemberErrorCode.USER_NOT_FOUND);
    }

    @Test
    @EnableSqlLogging
    @DisplayName("회원 정보 수정 후 즉시 조회 가능")
    void updateMember_ThenQuery_Success() {
        // given
        MemberUpdateRequest updateRequest = new MemberUpdateRequest(
                "ch_nick",
                "https://example.com/changed.jpg"
        );

        // when
        memberService.updateMember(TEST_MEMBER1_ID, updateRequest);
        MemberDetailsResponse response = memberService.getMemberProfile(TEST_MEMBER1_ID);

        // then
        assertThat(response.nickname()).isEqualTo("ch_nick");
        assertThat(response.profileImage()).isEqualTo("https://example.com/changed.jpg");
    }

    @Test
    @EnableSqlLogging
    @DisplayName("같은 닉네임으로 다시 변경 시도 - 성공 (자신의 닉네임)")
    void updateMember_SameNickname_Success() {
        // given - 현재 닉네임과 동일한 닉네임으로 변경
        MemberUpdateRequest request = new MemberUpdateRequest(
                "tester1",  // 현재 닉네임과 동일
                "https://example.com/new.jpg"
        );

        // when
        MemberUpdateResponse response = memberService.updateMember(TEST_MEMBER1_ID, request);

        // then
        assertThat(response.nickname()).isEqualTo("tester1");
        assertThat(response.profileImage()).isEqualTo("https://example.com/new.jpg");
    }
}
