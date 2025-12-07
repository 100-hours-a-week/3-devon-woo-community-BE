package com.devon.techblog.application.member.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.devon.techblog.application.member.MemberRequestFixture;
import com.devon.techblog.application.member.dto.request.MemberUpdateRequest;
import com.devon.techblog.application.member.dto.request.PasswordUpdateRequest;
import com.devon.techblog.application.member.dto.response.MemberDetailsResponse;
import com.devon.techblog.application.member.service.MemberService;
import com.devon.techblog.common.exception.BusinessException;
import com.devon.techblog.common.exception.code.MemberErrorCode;
import com.devon.techblog.config.annotation.ControllerWebMvcTest;
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

    @Test
    @DisplayName("회원 정보 수정 시 잘못된 프로필 이미지 URL - 400 Bad Request")
    void updateMember_withInvalidProfileImage_returns400() throws Exception {
        MemberUpdateRequest request = MemberRequestFixture.updateRequestWithInvalidProfileImage("invalid-url");

        mockMvc.perform(patch("/api/v1/members/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("비밀번호 변경 시 현재 비밀번호 누락 - 400 Bad Request")
    void updatePassword_withoutCurrentPassword_returns400() throws Exception {
        PasswordUpdateRequest request = MemberRequestFixture.passwordUpdateRequestWithoutCurrent();

        mockMvc.perform(patch("/api/v1/members/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("비밀번호 변경 시 새 비밀번호가 너무 짧음 - 400 Bad Request")
    void updatePassword_withShortNewPassword_returns400() throws Exception {
        PasswordUpdateRequest request = MemberRequestFixture.passwordUpdateRequestWithShortNew();

        mockMvc.perform(patch("/api/v1/members/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("존재하지 않는 회원 조회 - 404 Not Found")
    void getMemberProfile_notFound_returns404() throws Exception {
        willThrow(new BusinessException(MemberErrorCode.USER_NOT_FOUND))
                .given(memberService).getMemberProfile(any());

        mockMvc.perform(get("/api/v1/members/{memberId}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(MemberErrorCode.USER_NOT_FOUND.getMessage()));
    }
}
