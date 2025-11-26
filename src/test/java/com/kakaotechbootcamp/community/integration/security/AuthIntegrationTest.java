package com.kakaotechbootcamp.community.integration.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kakaotechbootcamp.community.application.security.util.JwtTokenProvider;
import com.kakaotechbootcamp.community.config.annotation.IntegrationTest;
import com.kakaotechbootcamp.community.domain.member.MemberFixture;
import com.kakaotechbootcamp.community.domain.member.entity.Member;
import com.kakaotechbootcamp.community.domain.member.repository.MemberRepository;
import com.kakaotechbootcamp.community.fake.FakeJwtTokenProvider;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
@Import(FakeJwtTokenProvider.class)
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private Member savedMember;
    private String refreshToken;
    private FakeJwtTokenProvider fakeJwtTokenProvider;

    @BeforeEach
    void setUp() {
        memberRepository.deleteAll();
        savedMember = memberRepository.save(MemberFixture.create(
                "tester@example.com",
                "password123",
                "tester"
        ));
        refreshToken = jwtTokenProvider.generateRefreshToken(savedMember.getId());
        fakeJwtTokenProvider = new FakeJwtTokenProvider();
    }

    @AfterEach
    void tearDown() {
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("통합 테스트 - 리프레시 토큰으로 액세스 토큰 갱신 시 200과 새 토큰을 반환한다")
    void refreshToken_returnsNewAccessToken_integration() throws Exception {
        Cookie refreshCookie = new Cookie("refreshToken", refreshToken);

        mockMvc.perform(post("/auth/refresh")
                        .cookie(refreshCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("토큰이 갱신되었습니다"))
                .andExpect(jsonPath("$.data.accessToken").isString());
    }

    @Test
    @DisplayName("통합 테스트 - 리프레시 토큰 없이 요청 시 예외를 반환한다")
    void refreshToken_withoutToken_throwsException() throws Exception {
        mockMvc.perform(post("/auth/refresh"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("통합 테스트 - 유효한 리프레시 토큰으로 여러 번 갱신 시 성공한다")
    void refreshToken_multipleTimes_succeeds() throws Exception {
        Cookie refreshCookie = new Cookie("refreshToken", refreshToken);

        mockMvc.perform(post("/auth/refresh")
                        .cookie(refreshCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isString());

        mockMvc.perform(post("/auth/refresh")
                        .cookie(refreshCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isString());
    }

    @Test
    @DisplayName("통합 테스트 - 만료된 리프레시 토큰으로 요청 시 예외를 반환한다")
    void refreshToken_withExpiredToken_throwsException() throws Exception {
        String expiredToken = fakeJwtTokenProvider.generateExpiredRefreshToken(savedMember.getId());
        Cookie expiredCookie = new Cookie("refreshToken", expiredToken);

        mockMvc.perform(post("/auth/refresh")
                        .cookie(expiredCookie))
                .andExpect(status().isUnauthorized());
    }
}
