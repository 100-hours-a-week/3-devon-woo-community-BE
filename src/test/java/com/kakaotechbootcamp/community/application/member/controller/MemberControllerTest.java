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
import com.kakaotechbootcamp.community.application.security.annotation.CurrentUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@WebMvcTest(MemberController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(MemberControllerTest.CurrentUserResolverConfig.class)
class MemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MemberService memberService;

    @TestConfiguration
    static class CurrentUserResolverConfig implements WebMvcConfigurer {

        @Bean
        public HandlerMethodArgumentResolver currentUserHandlerMethodArgumentResolver() {
            return new HandlerMethodArgumentResolver() {
                @Override
                public boolean supportsParameter(org.springframework.core.MethodParameter parameter) {
                    return parameter.hasParameterAnnotation(CurrentUser.class)
                            && parameter.getParameterType().equals(Long.class);
                }

                @Override
                public Object resolveArgument(org.springframework.core.MethodParameter parameter,
                                              org.springframework.web.method.support.ModelAndViewContainer mavContainer,
                                              org.springframework.web.context.request.NativeWebRequest webRequest,
                                              org.springframework.web.bind.support.WebDataBinderFactory binderFactory) {
                    return 1L;
                }
            };
        }
    }

    @Test
    @DisplayName("회원 정보를 조회하면 ApiResponse로 감싼 정보를 반환한다")
    void getMemberProfile_returnsResponse() throws Exception {
        MemberDetailsResponse response = new MemberDetailsResponse(1L, "tester", "user@test.com", "https://example.com/p.png");
        given(memberService.getMemberProfile(1L)).willReturn(response);

        mockMvc.perform(get("/api/v1/members/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.nickname").value("tester"))
                .andExpect(jsonPath("$.data.email").value("user@test.com"));

        verify(memberService).getMemberProfile(1L);
    }

    @Test
    @DisplayName("회원 정보를 수정하면 수정된 정보가 반환된다")
    void updateMember_returnsUpdatedResponse() throws Exception {
        MemberUpdateRequest request = new MemberUpdateRequest("newNick", "https://example.com/new.png");
        MemberUpdateResponse response = new MemberUpdateResponse("newNick", "https://example.com/new.png");
        given(memberService.updateMember(eq(1L), any(MemberUpdateRequest.class))).willReturn(response);

        mockMvc.perform(patch("/api/v1/members/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname", is("newNick")))
                .andExpect(jsonPath("$.data.profileImage", is("https://example.com/new.png")));

        verify(memberService).updateMember(eq(1L), any(MemberUpdateRequest.class));
    }

    @Test
    @DisplayName("비밀번호 변경 요청 시 204 응답을 반환한다")
    void updatePassword_returnsNoContent() throws Exception {
        PasswordUpdateRequest request = new PasswordUpdateRequest("currentPassword!", "newPassword123");

        mockMvc.perform(patch("/api/v1/members/{id}/password", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(memberService).updatePassword(eq(1L), any(PasswordUpdateRequest.class));
    }

    @Test
    @DisplayName("회원 탈퇴 요청 시 204 응답을 반환한다")
    void deleteMember_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/members/{id}", 1L))
                .andExpect(status().isNoContent());

        verify(memberService).deleteMember(1L);
    }
}
