package com.devon.techblog.application.security.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.devon.techblog.application.auth.SignupRequestFixture;
import com.devon.techblog.application.member.dto.request.SignupRequest;
import com.devon.techblog.application.security.dto.response.LoginResponse;
import com.devon.techblog.application.security.service.SignupService;
import com.devon.techblog.application.security.service.TokenBlacklistService;
import com.devon.techblog.application.security.service.TokenRefreshService;
import com.devon.techblog.application.security.util.CookieProvider;
import com.devon.techblog.common.exception.CustomException;
import com.devon.techblog.common.exception.code.MemberErrorCode;
import com.devon.techblog.config.annotation.ControllerWebMvcTest;
import com.devon.techblog.domain.member.repository.MemberRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@ControllerWebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TokenRefreshService tokenRefreshService;

    @MockitoBean
    private TokenBlacklistService tokenBlacklistService;

    @MockitoBean
    private CookieProvider cookieProvider;

    @MockitoBean
    private MemberRepository memberRepository;

    @MockitoBean
    private SignupService signupService;

    @Autowired
    private ObjectMapper objectMapper;


    @Test
    @DisplayName("회원가입 성공 - 201 Created")
    void signup_success() throws Exception {

        // given
        SignupRequest request = SignupRequestFixture.createRequest();
        LoginResponse response = new LoginResponse(1L, "fake-access-token");

        given(signupService.signup(any())).willReturn(response);

        // when & then
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.userId").value(1L));
    }

    @Test
    @DisplayName("회원가입 실패 - Validation 오류 시 400 Bad Request")
    void signup_validation_error() throws Exception {

        SignupRequest invalidRequest = SignupRequestFixture.createRequest("invalid-email-format", "1234", "devon", null);

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("회원가입 실패 - 중복 이메일 시 409 Conflict")
    void signup_duplicateEmail_returns409() throws Exception {
        SignupRequest request = SignupRequestFixture.createRequest();

        willThrow(new CustomException(MemberErrorCode.DUPLICATE_EMAIL))
                .given(signupService).signup(any());

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(MemberErrorCode.DUPLICATE_EMAIL.getMessage()));
    }

    @Test
    @DisplayName("회원가입 실패 - 중복 닉네임 시 409 Conflict")
    void signup_duplicateNickname_returns409() throws Exception {
        SignupRequest request = SignupRequestFixture.createRequest();

        willThrow(new CustomException(MemberErrorCode.DUPLICATE_NICKNAME))
                .given(signupService).signup(any());

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(MemberErrorCode.DUPLICATE_NICKNAME.getMessage()));
    }



    @Test
    @DisplayName("리프레시 토큰 쿠키가 있으면 새로운 액세스 토큰을 발급한다")
    void refreshToken_success() throws Exception {
        given(cookieProvider.getRefreshTokenFromCookie(any(HttpServletRequest.class)))
                .willReturn(Optional.of("refresh-token"));
        given(tokenRefreshService.refreshAccessToken("refresh-token"))
                .willReturn("new-access-token");

        mockMvc.perform(post("/auth/refresh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("토큰이 갱신되었습니다"))
                .andExpect(jsonPath("$.data.accessToken").value("new-access-token"));

        verify(tokenRefreshService).refreshAccessToken("refresh-token");
    }

    @Test
    @DisplayName("리프레시 토큰 쿠키가 없으면 401을 반환한다")
    void refreshToken_withoutCookie_returnsUnauthorized() throws Exception {
        given(cookieProvider.getRefreshTokenFromCookie(any(HttpServletRequest.class)))
                .willReturn(Optional.empty());

        mockMvc.perform(post("/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("리프레시 토큰을 찾을 수 없습니다"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("관리자가 토큰을 블랙리스트에 등록할 수 있다")
    void addTokenToBlacklist_adminSuccess() throws Exception {
        String body = "token-value";

        mockMvc.perform(post("/auth/blacklist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("토큰이 블랙리스트에 등록되었습니다"));

        verify(tokenBlacklistService).addToBlacklist("token-value");
    }

    @Test
    @DisplayName("이메일 중복 여부를 조회한다")
    void checkEmail_returnsAvailability() throws Exception {
        given(memberRepository.existsByEmail("user@example.com")).willReturn(false);

        mockMvc.perform(get("/auth/check-email")
                        .param("email", "user@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.available").value(true));
    }

    @Test
    @DisplayName("닉네임 중복 여부를 조회한다")
    void checkNickname_returnsAvailability() throws Exception {
        given(memberRepository.existsByNickname("nick")).willReturn(true);

        mockMvc.perform(get("/auth/check-nickname")
                        .param("nickname", "nick"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.available").value(false));
    }
}
