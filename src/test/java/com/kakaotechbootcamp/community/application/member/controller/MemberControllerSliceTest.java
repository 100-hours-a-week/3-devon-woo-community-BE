package com.kakaotechbootcamp.community.application.member.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
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
import com.kakaotechbootcamp.community.application.security.config.SecurityConfig;
import com.kakaotechbootcamp.community.application.security.filter.CustomLogoutFilter;
import com.kakaotechbootcamp.community.application.security.filter.FilterChainExceptionFilter;
import com.kakaotechbootcamp.community.application.security.filter.JwtAuthenticationFilter;
import com.kakaotechbootcamp.community.application.security.resolver.CurrentUserArgumentResolver;
import com.kakaotechbootcamp.community.config.TestSecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = MemberController.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                        SecurityConfig.class,
                        CurrentUserArgumentResolver.class,
                        CustomLogoutFilter.class,
                        JwtAuthenticationFilter.class,
                        FilterChainExceptionFilter.class
                })
        }
)
@AutoConfigureMockMvc(addFilters = false)
@Import({TestSecurityConfig.class, MemberControllerSliceTest.TestMvcConfig.class})
class MemberControllerSliceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MemberService memberService;

    static class TestMvcConfig {

        @Bean
        public MemberService memberService() {
            return org.mockito.Mockito.mock(MemberService.class);
        }
    }

    @Test
    @DisplayName("슬라이스 테스트 - 회원 정보 조회 시 ApiResponse로 감싼 정보를 반환한다")
    void getMemberProfile_returnsResponse_slice() throws Exception {
        MemberDetailsResponse response =
                new MemberDetailsResponse(1L, "tester", "user@test.com", "https://example.com/p.png");
        given(memberService.getMemberProfile(1L)).willReturn(response);

        mockMvc.perform(get("/api/v1/members/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("member_get_success"))
                .andExpect(jsonPath("$.data.nickname").value("tester"))
                .andExpect(jsonPath("$.data.email").value("user@test.com"));

        verify(memberService).getMemberProfile(1L);
    }

    @Test
    @DisplayName("슬라이스 테스트 - 회원 정보 수정 시 수정된 정보가 반환된다")
    void updateMember_returnsUpdatedResponse_slice() throws Exception {
        MemberUpdateRequest request =
                new MemberUpdateRequest("newNick", "https://example.com/new.png");
        MemberUpdateResponse response =
                new MemberUpdateResponse("newNick", "https://example.com/new.png");
        given(memberService.updateMember(eq(1L), any(MemberUpdateRequest.class))).willReturn(response);

        mockMvc.perform(patch("/api/v1/members/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("member_update_success"))
                .andExpect(jsonPath("$.data.nickname", is("newNick")))
                .andExpect(jsonPath("$.data.profileImage", is("https://example.com/new.png")));

        verify(memberService).updateMember(eq(1L), any(MemberUpdateRequest.class));
    }

    @Test
    @DisplayName("슬라이스 테스트 - 비밀번호 변경 요청 시 204 응답을 반환한다")
    void updatePassword_returnsNoContent_slice() throws Exception {
        PasswordUpdateRequest request = new PasswordUpdateRequest("currentPassword!", "newPassword123");

        mockMvc.perform(patch("/api/v1/members/{id}/password", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(memberService).updatePassword(eq(1L), any(PasswordUpdateRequest.class));
    }

    @Test
    @DisplayName("슬라이스 테스트 - 회원 탈퇴 요청 시 204 응답을 반환한다")
    void deleteMember_returnsNoContent_slice() throws Exception {
        mockMvc.perform(delete("/api/v1/members/{id}", 1L))
                .andExpect(status().isNoContent());

        verify(memberService).deleteMember(1L);
    }
}
