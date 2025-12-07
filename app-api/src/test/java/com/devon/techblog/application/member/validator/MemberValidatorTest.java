package com.devon.techblog.application.member.validator;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.devon.techblog.application.member.dto.request.PasswordUpdateRequest;
import com.devon.techblog.common.exception.BusinessException;
import com.devon.techblog.config.annotation.UnitTest;
import com.devon.techblog.domain.member.MemberFixture;
import com.devon.techblog.domain.member.entity.Member;
import com.devon.techblog.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@UnitTest
class MemberValidatorTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MemberValidator memberValidator;

    private Member member;

    @BeforeEach
    void setUp() {
        member = MemberFixture.createWithId(1L, "user@test.com", "password1234", "tester");
    }

    @Test
    @DisplayName("닉네임이 동일하면 중복 검사를 수행하지 않는다")
    void validateNicknameNotDuplicated_whenSameNickname_skipCheck() {
        memberValidator.validateNicknameNotDuplicated("tester", member);

        verify(memberRepository, never()).existsByNickname(anyString());
    }

    @Test
    @DisplayName("닉네임이 다르고 중복이면 예외가 발생한다")
    void validateNicknameNotDuplicated_whenDuplicated_throwsException() {
        given(memberRepository.existsByNickname("newNick")).willReturn(true);

        assertThatThrownBy(() -> memberValidator.validateNicknameNotDuplicated("newNick", member))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("닉네임이 다르고 중복이 아니면 검증을 통과한다")
    void validateNicknameNotDuplicated_whenAvailable_passes() {
        given(memberRepository.existsByNickname("newNick")).willReturn(false);

        assertThatCode(() -> memberValidator.validateNicknameNotDuplicated("newNick", member))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("현재 비밀번호가 일치하지 않으면 예외가 발생한다")
    void validatePasswordUpdate_invalidCurrentPassword() {
        PasswordUpdateRequest request = new PasswordUpdateRequest("wrong", "newPassword123");

        assertThatThrownBy(() -> memberValidator.validatePasswordUpdate(request, member))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("새 비밀번호가 기존과 동일하면 예외가 발생한다")
    void validatePasswordUpdate_sameAsCurrent() {
        PasswordUpdateRequest request = new PasswordUpdateRequest("password1234", "password1234");

        assertThatThrownBy(() -> memberValidator.validatePasswordUpdate(request, member))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("비밀번호가 조건을 충족하면 검증을 통과한다")
    void validatePasswordUpdate_valid() {
        PasswordUpdateRequest request = new PasswordUpdateRequest("password1234", "newPassword123");

        assertThatCode(() -> memberValidator.validatePasswordUpdate(request, member))
                .doesNotThrowAnyException();
    }
}
