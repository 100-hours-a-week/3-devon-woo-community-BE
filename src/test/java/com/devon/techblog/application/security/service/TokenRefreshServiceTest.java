package com.devon.techblog.application.security.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.devon.techblog.common.exception.CustomException;
import com.devon.techblog.common.exception.code.AuthErrorCode;
import com.devon.techblog.common.exception.code.MemberErrorCode;
import com.devon.techblog.config.annotation.UnitTest;
import com.devon.techblog.domain.member.MemberFixture;
import com.devon.techblog.domain.member.repository.MemberRepository;
import com.devon.techblog.application.security.util.JwtTokenProvider;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@UnitTest
class TokenRefreshServiceTest {

    private static final String VALID_REFRESH_TOKEN = "valid-refresh-token";
    private static final String INVALID_TOKEN = "invalid-token";

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private TokenRefreshService tokenRefreshService;

    private com.devon.techblog.domain.member.entity.Member activeMember;

    @BeforeEach
    void setUp() {
        activeMember = MemberFixture.createWithId(1L);
        activeMember.updateProfileImage("https://example.com/profile.png");
    }

    @Test
    @DisplayName("리프레시 토큰으로 새로운 액세스 토큰을 발급한다")
    void refreshAccessToken_success() {
        given(jwtTokenProvider.isRefreshToken(VALID_REFRESH_TOKEN)).willReturn(true);
        given(jwtTokenProvider.isTokenExpired(VALID_REFRESH_TOKEN)).willReturn(false);
        given(jwtTokenProvider.getUidFromToken(VALID_REFRESH_TOKEN)).willReturn(activeMember.getId());
        given(memberRepository.findById(activeMember.getId())).willReturn(Optional.of(activeMember));
        given(jwtTokenProvider.generateAccessToken(activeMember.getId(), activeMember.getRole().name()))
                .willReturn("fresh-access-token");

        String accessToken = tokenRefreshService.refreshAccessToken(VALID_REFRESH_TOKEN);

        assertThat(accessToken).isEqualTo("fresh-access-token");
    }

    @Test
    @DisplayName("리프레시 토큰이 아닌 경우 예외를 던진다")
    void refreshAccessToken_invalidTokenType() {
        given(jwtTokenProvider.isRefreshToken(INVALID_TOKEN)).willReturn(false);

        assertThatThrownBy(() -> tokenRefreshService.refreshAccessToken(INVALID_TOKEN))
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> assertThat(((CustomException) exception).getErrorCode())
                        .isEqualTo(AuthErrorCode.REFRESH_TOKEN_INVALID));
    }

    @Test
    @DisplayName("만료된 리프레시 토큰이면 예외를 던진다")
    void refreshAccessToken_expiredToken() {
        given(jwtTokenProvider.isRefreshToken(VALID_REFRESH_TOKEN)).willReturn(true);
        given(jwtTokenProvider.isTokenExpired(VALID_REFRESH_TOKEN)).willReturn(true);

        assertThatThrownBy(() -> tokenRefreshService.refreshAccessToken(VALID_REFRESH_TOKEN))
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> assertThat(((CustomException) exception).getErrorCode())
                        .isEqualTo(AuthErrorCode.REFRESH_TOKEN_EXPIRED));
    }

    @Test
    @DisplayName("존재하지 않는 회원이면 예외를 던진다")
    void refreshAccessToken_memberNotFound() {
        given(jwtTokenProvider.isRefreshToken(VALID_REFRESH_TOKEN)).willReturn(true);
        given(jwtTokenProvider.isTokenExpired(VALID_REFRESH_TOKEN)).willReturn(false);
        given(jwtTokenProvider.getUidFromToken(VALID_REFRESH_TOKEN)).willReturn(999L);
        given(memberRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> tokenRefreshService.refreshAccessToken(VALID_REFRESH_TOKEN))
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> assertThat(((CustomException) exception).getErrorCode())
                        .isEqualTo(MemberErrorCode.USER_NOT_FOUND));
    }

    @Test
    @DisplayName("비활성화된 회원이면 예외를 던진다")
    void refreshAccessToken_memberInactive() {
        var inactiveMember = MemberFixture.createWithId(2L);
        inactiveMember.deactivate();

        given(jwtTokenProvider.isRefreshToken(VALID_REFRESH_TOKEN)).willReturn(true);
        given(jwtTokenProvider.isTokenExpired(VALID_REFRESH_TOKEN)).willReturn(false);
        given(jwtTokenProvider.getUidFromToken(VALID_REFRESH_TOKEN)).willReturn(inactiveMember.getId());
        given(memberRepository.findById(inactiveMember.getId())).willReturn(Optional.of(inactiveMember));

        assertThatThrownBy(() -> tokenRefreshService.refreshAccessToken(VALID_REFRESH_TOKEN))
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> assertThat(((CustomException) exception).getErrorCode())
                        .isEqualTo(MemberErrorCode.MEMBER_INACTIVE));
    }
}
