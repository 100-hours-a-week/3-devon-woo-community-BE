package com.devon.techblog.application.member.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.devon.techblog.application.member.MemberRequestFixture;
import com.devon.techblog.application.member.dto.request.MemberUpdateRequest;
import com.devon.techblog.application.member.dto.request.PasswordUpdateRequest;
import com.devon.techblog.application.member.dto.response.MemberDetailsResponse;
import com.devon.techblog.application.member.service.MemberService;
import com.devon.techblog.config.annotation.ControllerWebMvcTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@ControllerWebMvcTest(MemberController.class)
class MemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MemberService memberService;

    @Test
    @DisplayName("회원 정보 조회 - 200 OK")
    void getMemberProfile_success() throws Exception {

        MemberDetailsResponse response = new MemberDetailsResponse(
                1L,
                "devon",
                "test@example.com",
                "https://example.com/profile.png",
                "USER",
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        given(memberService.getMemberProfile(any())).willReturn(response);

        mockMvc.perform(get("/api/v1/members/{memberId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.memberId").value(1L))
                .andExpect(jsonPath("$.data.nickname").value("devon"))
                .andExpect(jsonPath("$.data.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.profileImage").value("https://example.com/profile.png"));
    }

    @Test
    @DisplayName("회원 정보 수정 - 200 OK")
    void updateMember_returnsResponse() throws Exception {

        MemberUpdateRequest request = MemberRequestFixture.updateRequest("devon", "https://example.com/profile.png");
        MemberDetailsResponse response = new MemberDetailsResponse(
                1L,
                "devon",
                "test@example.com",
                "https://example.com/profile.png",
                "USER",
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        given(memberService.getMemberProfile(any())).willReturn(response);

        mockMvc.perform(patch("/api/v1/members/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("devon"))
                .andExpect(jsonPath("$.data.profileImage").value("https://example.com/profile.png"));
    }

    @Test
    @DisplayName("비밀번호 변경 - 204 No Content")
    void updatePassword_returnsNoContent() throws Exception {

        PasswordUpdateRequest request = new PasswordUpdateRequest("currentPassword!", "newPassword123");

        mockMvc.perform(patch("/api/v1/members/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("회원 탈퇴 - 204 No Content")
    void deleteMember_returnsNoContent() throws Exception {

        mockMvc.perform(delete("/api/v1/members/me"))
                .andExpect(status().isNoContent());
    }
}
