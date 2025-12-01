package com.devon.techblog.application.member.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.devon.techblog.application.member.dto.request.SignupRequest;
import com.devon.techblog.application.member.service.SignupService;
import com.devon.techblog.application.security.dto.response.LoginResponse;
import com.devon.techblog.common.exception.CustomException;
import com.devon.techblog.common.exception.code.MemberErrorCode;
import com.devon.techblog.config.annotation.ControllerWebMvcTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@ControllerWebMvcTest(SignupController.class)
class SignupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SignupService signupService;

    @Test
    @DisplayName("회원가입 성공 - 201 Created")
    void signup_success() throws Exception {

        // given
        SignupRequest request = new SignupRequest(
                "test@example.com",
                "password1234",
                "devon",
                null
        );

        LoginResponse response = new LoginResponse(
                1L,
                "fake-access-token"
        );

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

        SignupRequest invalidRequest = new SignupRequest(
                "invalid-email-format",
                "1234",
                "devon",
                null
        );

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("회원가입 실패 - 중복 이메일 시 409 Conflict")
    void signup_duplicateEmail_returns409() throws Exception {
        SignupRequest request = new SignupRequest(
                "test@example.com",
                "password1234",
                "devon",
                null
        );

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
        SignupRequest request = new SignupRequest(
                "test@example.com",
                "password1234",
                "devon",
                null
        );

        willThrow(new CustomException(MemberErrorCode.DUPLICATE_NICKNAME))
                .given(signupService).signup(any());

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(MemberErrorCode.DUPLICATE_NICKNAME.getMessage()));
    }
}
