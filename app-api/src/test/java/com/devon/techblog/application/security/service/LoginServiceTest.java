package com.devon.techblog.application.security.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.devon.techblog.config.annotation.UnitTest;
import com.devon.techblog.domain.member.MemberFixture;
import com.devon.techblog.domain.member.entity.Member;
import com.devon.techblog.domain.member.repository.MemberRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@UnitTest
class LoginServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private LoginService loginService;

    @Test
    @DisplayName("존재하는 활성화된 회원이면 CustomUserDetails를 반환한다")
    void loadUserByUsername_success() {
        Member member = MemberFixture.createWithId(1L);
        member.loginSuccess();
        given(memberRepository.findByEmail(member.getEmail())).willReturn(Optional.of(member));

        var userDetails = loginService.loadUserByUsername(member.getEmail());

        assertThat(userDetails.getUsername()).isEqualTo(member.getId().toString());
        assertThat(userDetails).isInstanceOf(com.devon.techblog.application.security.dto.user.CustomUserDetails.class);
    }

    @Test
    @DisplayName("존재하지 않는 이메일이면 UsernameNotFoundException을 던진다")
    void loadUserByUsername_notFound() {
        given(memberRepository.findByEmail("unknown@example.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> loginService.loadUserByUsername("unknown@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    @DisplayName("비활성화된 계정이면 DisabledException을 던진다")
    void loadUserByUsername_disabled() {
        Member member = MemberFixture.createWithId(2L);
        member.deactivate();
        given(memberRepository.findByEmail(member.getEmail())).willReturn(Optional.of(member));

        assertThatThrownBy(() -> loginService.loadUserByUsername(member.getEmail()))
                .isInstanceOf(DisabledException.class);
    }
}
