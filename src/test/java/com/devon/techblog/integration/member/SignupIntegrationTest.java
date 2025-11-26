package com.devon.techblog.integration.member;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.devon.techblog.application.member.dto.request.SignupRequest;
import com.devon.techblog.config.annotation.IntegrationTest;
import com.devon.techblog.domain.member.repository.MemberRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
class SignupIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MemberRepository memberRepository;

    @BeforeEach
    void setUp() {
        memberRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("통합 테스트 - 회원가입 시 201과 생성된 회원 ID를 반환한다")
    void signup_returnsCreated_integration() throws Exception {
        SignupRequest request = new SignupRequest(
                "newuser@example.com",
                "password123",
                "newuser",
                "https://example.com/profile.jpg"
        );

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("signup_success"))
                .andExpect(jsonPath("$.data.userId").isNumber());

        Assertions.assertThat(memberRepository.count()).isEqualTo(1);
        Assertions.assertThat(memberRepository.findByEmail("newuser@example.com")).isPresent();
    }

    @Test
    @DisplayName("통합 테스트 - 프로필 이미지 없이 회원가입 시 201을 반환한다")
    void signup_withoutProfileImage_returnsCreated() throws Exception {
        SignupRequest request = new SignupRequest(
                "user@example.com",
                "password123",
                "user",
                null
        );

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").isNumber());

        Assertions.assertThat(memberRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("통합 테스트 - 여러 회원 가입 시 각각 저장된다")
    void signupMultipleUsers_savesEach() throws Exception {
        SignupRequest request1 = new SignupRequest(
                "user1@example.com",
                "password123",
                "user1",
                null
        );

        SignupRequest request2 = new SignupRequest(
                "user2@example.com",
                "password123",
                "user2",
                null
        );

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isCreated());

        Assertions.assertThat(memberRepository.count()).isEqualTo(2);
        Assertions.assertThat(memberRepository.findByEmail("user1@example.com")).isPresent();
        Assertions.assertThat(memberRepository.findByEmail("user2@example.com")).isPresent();
    }
}
