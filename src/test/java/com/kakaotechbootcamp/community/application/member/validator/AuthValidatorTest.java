package com.kakaotechbootcamp.community.application.member.validator;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.kakaotechbootcamp.community.application.member.dto.request.SignupRequest;
import com.kakaotechbootcamp.community.common.exception.CustomException;
import com.kakaotechbootcamp.community.config.UnitTest;
import com.kakaotechbootcamp.community.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
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

    private SignupRequest request;

    @BeforeEach
    void setUp() {
        request = new SignupRequest(
                "user@test.com",
                "password1234",
                "tester",
                "https://example.com/profile.png"
        );
    }

    @Test
    @DisplayName("이메일 또는 닉네임이 중복되지 않으면 회원가입 검증을 통과한다")
    void validateSignup_whenUnique_passes() {
        given(memberRepository.existsByEmail("user@test.com")).willReturn(false);
        given(memberRepository.existsByNickname("tester")).willReturn(false);

        assertThatCode(() -> authValidator.validateSignup(request)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("이메일이 중복되면 예외가 발생한다")
    void validateSignup_whenEmailDuplicate_throwsException() {
        given(memberRepository.existsByEmail("user@test.com")).willReturn(true);

        assertThatThrownBy(() -> authValidator.validateSignup(request))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("닉네임이 중복되면 예외가 발생한다")
    void validateSignup_whenNicknameDuplicate_throwsException() {
        given(memberRepository.existsByNickname("tester")).willReturn(true);

        assertThatThrownBy(() -> authValidator.validateSignup(request))
                .isInstanceOf(CustomException.class);
    }
}
