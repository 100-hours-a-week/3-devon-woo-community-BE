package com.kakaotechbootcamp.community.application.member.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kakaotechbootcamp.community.application.member.dto.request.SignupRequest;
import com.kakaotechbootcamp.community.application.member.dto.response.SignupResponse;
import com.kakaotechbootcamp.community.application.member.service.SignupService;
import com.kakaotechbootcamp.community.config.ControllerWebMvcTest;
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

        SignupResponse response = new SignupResponse(
                1L
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

        // given — 잘못된 이메일 값
        SignupRequest invalidRequest = new SignupRequest(
                "invalid-email-format",
                "1234",
                "devon",
                null
        );

        // when & then
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
}
