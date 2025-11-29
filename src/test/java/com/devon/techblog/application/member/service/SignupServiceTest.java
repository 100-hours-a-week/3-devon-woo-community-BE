package com.devon.techblog.application.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.devon.techblog.application.member.dto.request.SignupRequest;
import com.devon.techblog.application.member.validator.AuthValidator;
import com.devon.techblog.application.security.dto.response.LoginResponse;
import com.devon.techblog.application.security.util.JwtTokenProvider;
import com.devon.techblog.config.annotation.UnitTest;
import com.devon.techblog.domain.member.entity.Member;
import com.devon.techblog.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@UnitTest
class SignupServiceTest {

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private AuthValidator authValidator;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private SignupService signupService;

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
    @DisplayName("회원가입 시 요청을 검증하고 패스워드를 인코딩한 뒤 저장한다")
    void signup_createsMember() {
        given(passwordEncoder.encode("password1234")).willReturn("encodedPassword");
        given(memberRepository.save(any(Member.class))).willAnswer(invocation -> {
            Member member = invocation.getArgument(0);
            ReflectionTestUtils.setField(member, "id", 1L);
            return member;
        });
        given(jwtTokenProvider.generateAccessToken(any(), any())).willReturn("fake-token");

        LoginResponse response = signupService.signup(request);

        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.accessToken()).isEqualTo("fake-token");
    }
}
