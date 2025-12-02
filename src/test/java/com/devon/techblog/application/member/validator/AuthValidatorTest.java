package com.devon.techblog.application.member.validator;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.devon.techblog.application.auth.SignupRequestFixture;
import com.devon.techblog.application.member.dto.request.SignupRequest;
import com.devon.techblog.common.exception.CustomException;
import com.devon.techblog.config.annotation.UnitTest;
import com.devon.techblog.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@UnitTest
class AuthValidatorTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private AuthValidator authValidator;


    @Test
    @DisplayName("이메일 또는 닉네임이 중복되지 않으면 회원가입 검증을 통과한다")
    void validateSignup_whenUnique_passes() {
        SignupRequest request = SignupRequestFixture.createRequest();

        given(memberRepository.existsByEmail(SignupRequestFixture.DEFAULT_EMAIL)).willReturn(false);
        given(memberRepository.existsByNickname(SignupRequestFixture.DEFAULT_NICKNAME)).willReturn(false);

        assertThatCode(() -> authValidator.validateSignup(request)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("이메일이 중복되면 예외가 발생한다")
    void validateSignup_whenEmailDuplicate_throwsException() {
        SignupRequest request = SignupRequestFixture.createRequest();
        given(memberRepository.existsByEmail(SignupRequestFixture.DEFAULT_EMAIL)).willReturn(true);

        assertThatThrownBy(() -> authValidator.validateSignup(request))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("닉네임이 중복되면 예외가 발생한다")
    void validateSignup_whenNicknameDuplicate_throwsException() {
        SignupRequest request = SignupRequestFixture.createRequest();

        given(memberRepository.existsByNickname(SignupRequestFixture.DEFAULT_NICKNAME)).willReturn(true);

        assertThatThrownBy(() -> authValidator.validateSignup(request))
                .isInstanceOf(CustomException.class);
    }
}
