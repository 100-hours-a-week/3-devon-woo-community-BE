package com.kakaotechbootcamp.community.application.member.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kakaotechbootcamp.community.application.member.dto.request.MemberUpdateRequest;
import com.kakaotechbootcamp.community.application.member.dto.request.PasswordUpdateRequest;
import com.kakaotechbootcamp.community.application.member.dto.response.MemberDetailsResponse;
import com.kakaotechbootcamp.community.application.member.dto.response.MemberUpdateResponse;
import com.kakaotechbootcamp.community.application.member.service.MemberService;
import com.kakaotechbootcamp.community.config.ControllerWebMvcTest;
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
                "https://example.com/profile.png"
        );

        given(memberService.getMemberProfile(any())).willReturn(response);

        mockMvc.perform(get("/api/v1/members/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.memberId").value(1L))
                .andExpect(jsonPath("$.data.nickname").value("devon"))
                .andExpect(jsonPath("$.data.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.profileImage").value("https://example.com/profile.png"));
    }

    @Test
    @DisplayName("회원 정보 수정 - 200 OK")
    void updateMember_returnsResponse() throws Exception {

        MemberUpdateRequest request = new MemberUpdateRequest("devon", "https://example.com/profile.png");
        MemberUpdateResponse response = new MemberUpdateResponse("devon", "https://example.com/profile.png");

        given(memberService.updateMember(any(), any())).willReturn(response);

        mockMvc.perform(patch("/api/v1/members/{id}", 1L)
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

        mockMvc.perform(patch("/api/v1/members/{id}/password", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("회원 탈퇴 - 204 No Content")
    void deleteMember_returnsNoContent() throws Exception {

        mockMvc.perform(delete("/api/v1/members/{id}", 1L))
                .andExpect(status().isNoContent());
    }
}
