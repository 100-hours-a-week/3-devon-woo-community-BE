package com.devon.techblog.integration.member;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.devon.techblog.application.member.MemberRequestFixture;
import com.devon.techblog.application.member.dto.request.MemberUpdateRequest;
import com.devon.techblog.application.member.dto.request.PasswordUpdateRequest;
import com.devon.techblog.config.TestCurrentUserContext;
import com.devon.techblog.config.annotation.IntegrationTest;
import com.devon.techblog.domain.member.MemberFixture;
import com.devon.techblog.domain.member.entity.Member;
import com.devon.techblog.domain.member.entity.MemberStatus;
import com.devon.techblog.domain.member.repository.MemberRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
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
        savedMember = memberRepository.save(MemberFixture.create(
                "tester@example.com",
                "currentPassword!",
                "tester"
        ));
        currentUserContext.setCurrentUserId(savedMember.getId());
    }

    @AfterEach
    void tearDown() {
        currentUserContext.clear();
    }

    @Test
    @DisplayName("통합 테스트 - 회원 정보 조회 시 ApiResponse로 감싼 정보를 반환한다")
    void getMemberProfile_returnsResponse_integration() throws Exception {
        mockMvc.perform(get("/api/v1/members/me"))
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

        mockMvc.perform(patch("/api/v1/members/me")
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

        mockMvc.perform(patch("/api/v1/members/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        Member updated = memberRepository.findById(savedMember.getId()).orElseThrow();
        Assertions.assertThat(updated.getPassword()).isEqualTo("newPassword123");
    }

    @Test
    @DisplayName("통합 테스트 - 회원 탈퇴 요청 시 204 응답을 반환한다")
    void deleteMember_returnsNoContent_integration() throws Exception {
        mockMvc.perform(delete("/api/v1/members/me"))
                .andExpect(status().isNoContent());

        Member deleted = memberRepository.findById(savedMember.getId()).orElseThrow();
        Assertions.assertThat(deleted.getStatus()).isEqualTo(MemberStatus.WITHDRAWN);
    }

    @Test
    @DisplayName("통합 테스트 - 여러 회원 생성 후 각각 조회할 수 있다")
    void getMultipleMembers_returnsEachProfile() throws Exception {
        Member member2 = memberRepository.save(MemberFixture.create(
                "tester2@example.com",
                "password123",
                "tester2"
        ));
        Member member3 = memberRepository.save(MemberFixture.create(
                "tester3@example.com",
                "password123",
                "tester3"
        ));

        currentUserContext.setCurrentUserId(member2.getId());
        mockMvc.perform(get("/api/v1/members/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("tester2"))
                .andExpect(jsonPath("$.data.email").value("tester2@example.com"));

        currentUserContext.setCurrentUserId(member3.getId());
        mockMvc.perform(get("/api/v1/members/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("tester3"))
                .andExpect(jsonPath("$.data.email").value("tester3@example.com"));

        Assertions.assertThat(memberRepository.count()).isEqualTo(3);
    }

    @Test
    @DisplayName("통합 테스트 - 회원 정보 수정 후 조회 시 변경된 정보를 반환한다")
    void updateMemberThenGet_returnsUpdatedInfo() throws Exception {
        MemberUpdateRequest updateRequest = MemberRequestFixture.updateRequest();

        mockMvc.perform(patch("/api/v1/members/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/members/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("newNick"))
                .andExpect(jsonPath("$.data.profileImage").value("https://example.com/new.png"));
    }

    @Test
    @DisplayName("통합 테스트 - 비밀번호 변경 후 새 비밀번호로 변경할 수 있다")
    void updatePasswordTwice_succeeds() throws Exception {
        PasswordUpdateRequest firstUpdate = new PasswordUpdateRequest("currentPassword!", "newPassword123");

        mockMvc.perform(patch("/api/v1/members/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstUpdate)))
                .andExpect(status().isNoContent());

        PasswordUpdateRequest secondUpdate = new PasswordUpdateRequest("newPassword123", "finalPassword456");

        mockMvc.perform(patch("/api/v1/members/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondUpdate)))
                .andExpect(status().isNoContent());

        Member updated = memberRepository.findById(savedMember.getId()).orElseThrow();
        Assertions.assertThat(updated.getPassword()).isEqualTo("finalPassword456");
    }

    @Test
    @DisplayName("통합 테스트 - 존재하지 않는 회원 조회 시 404를 반환한다")
    void getMemberProfile_notFound_returns404() throws Exception {
        Long nonExistentId = 99999L;
        currentUserContext.setCurrentUserId(nonExistentId);

        mockMvc.perform(get("/api/v1/members/me"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("통합 테스트 - 잘못된 비밀번호로 변경 시도 시 400을 반환한다")
    void updatePassword_withWrongCurrentPassword_returns400() throws Exception {
        PasswordUpdateRequest request = new PasswordUpdateRequest("wrongPassword!", "newPassword123");

        mockMvc.perform(patch("/api/v1/members/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }


    // Todo: 예외보다는 다른 처리 방법이 있을까?
    @Test
    @DisplayName("통합 테스트 - 탈퇴한 회원 조회 시 404 예외를 던진다")
    void getDeletedMember_handlesAppropriately() throws Exception {
        mockMvc.perform(delete("/api/v1/members/me"))
                .andExpect(status().isNoContent());

        Member deleted = memberRepository.findById(savedMember.getId()).orElseThrow();
        Assertions.assertThat(deleted.getStatus()).isEqualTo(MemberStatus.WITHDRAWN);

        mockMvc.perform(get("/api/v1/members/me"))
                .andExpect(status().isNotFound());
        // todo : 예외 설명도 넣으면 더 좋지 않을까?
    }

}
