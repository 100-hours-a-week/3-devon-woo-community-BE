package com.devon.techblog.integration.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.devon.techblog.application.security.service.TokenBlacklistService;
import com.devon.techblog.application.security.util.JwtTokenProvider;
import com.devon.techblog.config.RedisMockConfig;
import com.devon.techblog.config.annotation.IntegrationTest;
import com.devon.techblog.domain.member.MemberFixture;
import com.devon.techblog.domain.member.entity.Member;
import com.devon.techblog.domain.member.repository.MemberRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
@Import(RedisMockConfig.class)
class AuthLogoutIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private TokenBlacklistService tokenBlacklistService;

    private Member savedMember;
    private String refreshToken;

    @BeforeEach
    void setUp() {
        savedMember = memberRepository.save(MemberFixture.create(
                "logout@example.com",
                "password123",
                "logout"
        ));
        refreshToken = jwtTokenProvider.generateRefreshToken(savedMember.getId());
    }

    @Test
    @DisplayName("로그아웃 시 리프레시 토큰이 블랙리스트에 등록되고 쿠키가 삭제된다")
    void logout_blacklistsTokenAndDeletesCookie() throws Exception {
        Cookie refreshCookie = new Cookie("refreshToken", refreshToken);
        given(tokenBlacklistService.isBlacklisted(refreshToken)).willReturn(false);

        MockHttpServletResponse logoutResponse = mockMvc.perform(post("/auth/logout")
                        .cookie(refreshCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("로그아웃되었습니다."))
                .andReturn()
                .getResponse();

        verify(tokenBlacklistService).addToBlacklist(refreshToken);
        assertThat(logoutResponse.getHeader("Set-Cookie")).contains("Max-Age=0");
    }

    @Test
    @DisplayName("로그아웃한 토큰으로 리프레시 요청 시 401을 반환한다")
    void refresh_withBlacklistedToken_returnsUnauthorized() throws Exception {
        Cookie refreshCookie = new Cookie("refreshToken", refreshToken);

        mockMvc.perform(post("/auth/logout")
                        .cookie(refreshCookie))
                .andExpect(status().isOk());

        given(tokenBlacklistService.isBlacklisted(refreshToken)).willReturn(true);
        mockMvc.perform(post("/auth/refresh")
                        .cookie(refreshCookie))
                .andExpect(status().isUnauthorized());
    }

}
