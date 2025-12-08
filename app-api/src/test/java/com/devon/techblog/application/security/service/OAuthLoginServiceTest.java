package com.devon.techblog.application.security.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.devon.techblog.application.security.dto.user.CustomOAuthUserDetails;
import com.devon.techblog.config.RedisMockConfig;
import com.devon.techblog.domain.member.entity.Member;
import com.devon.techblog.domain.member.entity.MemberRole;
import com.devon.techblog.domain.member.entity.oauth.OAuthMember;
import com.devon.techblog.domain.member.repository.MemberRepository;
import com.devon.techblog.domain.member.repository.OAuthMemberRepository;
import com.devon.techblog.fake.FakeOAuth2Provider;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@Import(RedisMockConfig.class)
@DisplayName("OAuthLoginService 통합 테스트")
class OAuthLoginServiceTest {

    private static final FakeOAuth2Provider fakeGoogle = FakeOAuth2Provider.google();

    @DynamicPropertySource
    static void registerOauthProvider(DynamicPropertyRegistry registry) {
        fakeGoogle.start();
        fakeGoogle.register(registry);
    }

    @Autowired
    private OAuthLoginService oAuthLoginService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private OAuthMemberRepository oAuthMemberRepository;

    @Autowired
    private ClientRegistrationRepository clientRegistrationRepository;

    @AfterEach
    void cleanup() {
        oAuthMemberRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @AfterAll
    static void shutdownFakeProvider() throws IOException {
        fakeGoogle.close();
    }

    @Test
    @DisplayName("신규 사용자: OAuth2 로그인 시 Member와 OAuthMember를 생성하고 CustomOAuthUserDetails를 반환한다")
    void loadUser_newUser_createsMemberAndOAuthMember() {
        ClientRegistration clientRegistration = clientRegistrationRepository.findByRegistrationId("google");
        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "fake-access-token",
                null,
                null
        );
        OAuth2UserRequest userRequest = new OAuth2UserRequest(clientRegistration, accessToken);

        OAuth2User result = oAuthLoginService.loadUser(userRequest);

        assertThat(result).isInstanceOf(CustomOAuthUserDetails.class);
        CustomOAuthUserDetails userDetails = (CustomOAuthUserDetails) result;
        assertThat(userDetails.getAttributes().get("role")).isEqualTo(MemberRole.USER.name());

        assertThat(memberRepository.count()).isEqualTo(1);
        assertThat(oAuthMemberRepository.count()).isEqualTo(1);

        Optional<OAuthMember> savedOAuthMember = oAuthMemberRepository.findByProviderAndProviderId("google", "fake-google-sub");
        assertThat(savedOAuthMember).isPresent();
        assertThat(savedOAuthMember.get().getMember().getEmail()).isEqualTo("google-user@example.com");
        assertThat(savedOAuthMember.get().getMember().getNickname()).isEqualTo("Fake ");
        assertThat(savedOAuthMember.get().getMember().getRole()).isEqualTo(MemberRole.USER);
    }

    @Test
    @DisplayName("기존 사용자: OAuth2 로그인 시 기존 OAuthMember를 조회하고 lastLoginAt을 갱신한다")
    void loadUser_existingUser_updatesLastLoginAt() {
        Member existingMember = Member.create("existing@example.com", "encoded-password", "Exist");
        memberRepository.save(existingMember);
        OAuthMember existingOAuthMember = OAuthMember.create("google", "fake-google-sub", existingMember);
        oAuthMemberRepository.save(existingOAuthMember);

        ClientRegistration clientRegistration = clientRegistrationRepository.findByRegistrationId("google");
        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "fake-access-token",
                null,
                null
        );
        OAuth2UserRequest userRequest = new OAuth2UserRequest(clientRegistration, accessToken);

        OAuth2User result = oAuthLoginService.loadUser(userRequest);

        assertThat(result).isInstanceOf(CustomOAuthUserDetails.class);
        assertThat(memberRepository.count()).isEqualTo(1);
        assertThat(oAuthMemberRepository.count()).isEqualTo(1);

        Member updatedMember = memberRepository.findById(existingMember.getId()).get();
        assertThat(updatedMember.getLastLoginAt()).isNotNull();
    }
}
