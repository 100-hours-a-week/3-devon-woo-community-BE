package com.kakaotechbootcamp.community.integration.member;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kakaotechbootcamp.community.application.member.MemberRequestFixture;
import com.kakaotechbootcamp.community.application.member.dto.request.MemberUpdateRequest;
import com.kakaotechbootcamp.community.application.member.dto.request.PasswordUpdateRequest;
import com.kakaotechbootcamp.community.config.annotation.IntegrationTest;
import com.kakaotechbootcamp.community.config.TestSecurityConfig;
import com.kakaotechbootcamp.community.config.TestSecurityConfig.TestCurrentUserContext;
import com.kakaotechbootcamp.community.domain.member.MemberFixture;
import com.kakaotechbootcamp.community.domain.member.entity.Member;
import com.kakaotechbootcamp.community.domain.member.entity.MemberStatus;
import com.kakaotechbootcamp.community.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.assertj.core.api.Assertions;

@IntegrationTest
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
class MemberIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private TestCurrentUserContext currentUserContext;

    private Member savedMember;

    @BeforeEach
    void setUp() {
        memberRepository.deleteAll();
        savedMember = memberRepository.save(MemberFixture.create(
                "tester@example.com",
                "currentPassword!",
                "tester"
        ));
        currentUserContext.setCurrentUserId(savedMember.getId());
    }

    @AfterEach
    void tearDown() {
        memberRepository.deleteAll();
        currentUserContext.clear();
    }

    @Test
    @DisplayName("통합 테스트 - 회원 정보 조회 시 ApiResponse로 감싼 정보를 반환한다")
    void getMemberProfile_returnsResponse_integration() throws Exception {
        mockMvc.perform(get("/api/v1/members/{id}", savedMember.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("member_get_success"))
                .andExpect(jsonPath("$.data.nickname").value("tester"))
                .andExpect(jsonPath("$.data.email").value("tester@example.com"));
    }

    @Test
    @DisplayName("통합 테스트 - 회원 정보 수정 시 수정된 정보가 반환된다")
    void updateMember_returnsUpdatedResponse_integration() throws Exception {
        MemberUpdateRequest request = MemberRequestFixture.updateRequest();

        mockMvc.perform(patch("/api/v1/members/{id}", savedMember.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("member_update_success"))
                .andExpect(jsonPath("$.data.nickname", is("newNick")))
                .andExpect(jsonPath("$.data.profileImage", is("https://example.com/new.png")));

        Member updated = memberRepository.findById(savedMember.getId()).orElseThrow();
        Assertions.assertThat(updated.getNickname()).isEqualTo("newNick");
        Assertions.assertThat(updated.getProfileImageUrl()).isEqualTo("https://example.com/new.png");
    }

    @Test
    @DisplayName("통합 테스트 - 비밀번호 변경 요청 시 204 응답을 반환한다")
    void updatePassword_returnsNoContent_integration() throws Exception {
        PasswordUpdateRequest request = new PasswordUpdateRequest("currentPassword!", "newPassword123");

        mockMvc.perform(patch("/api/v1/members/{id}/password", savedMember.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        Member updated = memberRepository.findById(savedMember.getId()).orElseThrow();
        Assertions.assertThat(updated.getPassword()).isEqualTo("newPassword123");
    }

    @Test
    @DisplayName("통합 테스트 - 회원 탈퇴 요청 시 204 응답을 반환한다")
    void deleteMember_returnsNoContent_integration() throws Exception {
        mockMvc.perform(delete("/api/v1/members/{id}", savedMember.getId()))
                .andExpect(status().isNoContent());

        Member deleted = memberRepository.findById(savedMember.getId()).orElseThrow();
        Assertions.assertThat(deleted.getStatus()).isEqualTo(MemberStatus.WITHDRAWN);
    }
}
