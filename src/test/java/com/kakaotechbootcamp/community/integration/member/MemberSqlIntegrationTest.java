package com.kakaotechbootcamp.community.integration.member;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kakaotechbootcamp.community.config.TestSecurityConfig.TestCurrentUserContext;
import com.kakaotechbootcamp.community.config.annotation.IntegrationTest;
import com.kakaotechbootcamp.community.domain.member.entity.Member;
import com.kakaotechbootcamp.community.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
class MemberSqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private TestCurrentUserContext currentUserContext;

    @AfterEach
    void tearDown() {
        memberRepository.deleteAll();
        currentUserContext.clear();
    }

    @Test
    @Sql("/sql/member-test-data.sql")
    @DisplayName("SQL 스크립트로 생성된 회원 정보를 API로 조회할 수 있다")
    void getMemberProfile_withSqlScript_returnsResponse() throws Exception {
        Member test1 = memberRepository.findAll().stream()
                .filter(m -> m.getEmail().equals("test1@example.com"))
                .findFirst()
                .orElseThrow();

        currentUserContext.setCurrentUserId(test1.getId());

        mockMvc.perform(get("/api/v1/members/{id}", test1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.nickname").value("tester1"))
                .andExpect(jsonPath("$.data.email").value("test1@example.com"));
    }

    @Test
    @Sql("/sql/member-test-data.sql")
    @DisplayName("SQL 스크립트로 생성된 Admin 회원 정보를 API로 조회할 수 있다")
    void getAdminProfile_withSqlScript_returnsResponse() throws Exception {
        Member admin = memberRepository.findAll().stream()
                .filter(m -> m.getEmail().equals("admin@example.com"))
                .findFirst()
                .orElseThrow();

        currentUserContext.setCurrentUserId(admin.getId());

        mockMvc.perform(get("/api/v1/members/{id}", admin.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.nickname").value("admin"))
                .andExpect(jsonPath("$.data.email").value("admin@example.com"));
    }
}
