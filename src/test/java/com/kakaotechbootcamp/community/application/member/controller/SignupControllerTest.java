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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SignupController.class)
@AutoConfigureMockMvc(addFilters = false)
class SignupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SignupService signupService;

    @Test
    @DisplayName("회원가입 요청 시 201 응답과 가입 정보를 반환한다")
    void signUp_returnsCreatedResponse() throws Exception {
        SignupRequest request = new SignupRequest(
                "user@test.com",
                "password1234",
                "tester",
                "https://example.com/profile.png"
        );
        given(signupService.signup(any(SignupRequest.class))).willReturn(new SignupResponse(1L));

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("signup_success"))
                .andExpect(jsonPath("$.data.userId").value(1L));
    }
}
