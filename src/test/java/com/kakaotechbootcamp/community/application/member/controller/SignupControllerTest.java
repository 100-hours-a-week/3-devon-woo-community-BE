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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class SignupControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private SignupService signupService;

    @InjectMocks
    private SignupController signupController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(signupController).build();
    }

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
