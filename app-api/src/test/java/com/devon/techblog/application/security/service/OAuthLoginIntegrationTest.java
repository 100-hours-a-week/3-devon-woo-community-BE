package com.devon.techblog.application.security.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.devon.techblog.application.security.constants.CookieConstants;
import com.devon.techblog.common.utils.SerializationUtil;
import com.devon.techblog.config.RedisMockConfig;
import com.devon.techblog.domain.member.entity.oauth.OAuthMember;
import com.devon.techblog.domain.member.repository.MemberRepository;
import com.devon.techblog.domain.member.repository.OAuthMemberRepository;
import com.devon.techblog.fake.FakeOAuth2Provider;
import jakarta.servlet.http.Cookie;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(RedisMockConfig.class)
@DisplayName("OAuth2 로그인 통합 테스트")
class OAuthLoginIntegrationTest {

    private static final FakeOAuth2Provider fakeGoogle = FakeOAuth2Provider.google();

    @DynamicPropertySource
    static void registerOauthProvider(DynamicPropertyRegistry registry) {
        fakeGoogle.start();
        fakeGoogle.register(registry);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OAuthMemberRepository oAuthMemberRepository;

    @Autowired
    private MemberRepository memberRepository;

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
    @DisplayName("OAuth2 구글 로그인 흐름: 사용자·OAuth 연동 정보 생성 및 리프레시 쿠키 발급")
    void oauth2Login_successful() throws Exception {
        // [1단계] Authorization Request: 클라이언트가 OAuth2 인증 시작
        // GET /oauth2/authorization/google 호출 시 Spring Security가 OAuth2 Provider로 리다이렉트
        MvcResult authorizationResult = mockMvc.perform(MockMvcRequestBuilders.get("/oauth2/authorization/google"))
                .andReturn();

        // Authorization URL 추출 (OAuth2 Provider의 인증 페이지 URL)
        String authorizationLocation = authorizationResult.getResponse().getHeader(HttpHeaders.LOCATION);

        // OAuth2 상태 관리를 위한 쿠키 추출 (CSRF 방지 및 요청 검증용)
        Cookie[] authCookies = Optional.ofNullable(authorizationResult.getResponse().getCookies()).orElse(new Cookie[0]);
        String storedState = Arrays.stream(authCookies)
                .filter(cookie -> CookieConstants.OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME.equals(cookie.getName()))
                .findFirst()
                .map(cookie -> SerializationUtil.deserialize(cookie.getValue(), OAuth2AuthorizationRequest.class).getState())
                .orElseThrow(() -> new IllegalStateException("OAuth2 상태 쿠키를 찾을 수 없습니다."));

        assertThat(authorizationLocation).isNotBlank();

        // [2단계] Provider Authorization: Fake OAuth2 Provider에 인증 요청
        // 실제 환경에서는 구글 로그인 페이지로 이동하지만, 테스트에서는 Fake Provider 사용
        OkHttpClient client = new OkHttpClient.Builder()
                .followRedirects(false)
                .build();

        Request providerRequest = new Request.Builder()
                .url(authorizationLocation)
                .get()
                .build();

        try (Response providerResponse = client.newCall(providerRequest).execute()) {
            // Provider가 인증 성공 후 302 리다이렉트로 응답
            assertThat(providerResponse.code()).isEqualTo(302);

            // [3단계] Authorization Code 획득: Provider가 콜백 URL로 리다이렉트하며 code 전달
            String callbackLocation = providerResponse.header(HttpHeaders.LOCATION);
            UriComponents callbackUri = UriComponentsBuilder.fromUriString(callbackLocation).build(true);
            String code = callbackUri.getQueryParams().getFirst("code");

            assertThat(code).isNotBlank();

            // [4단계] Callback 처리: Authorization Code를 사용하여 토큰 교환 및 사용자 정보 조회
            // Spring Security가 자동으로:
            // 1) code를 access_token으로 교환
            // 2) access_token으로 사용자 정보 조회 (OAuthLoginService.loadUser 호출)
            // 3) 사용자 생성 또는 로그인 처리
            // 4) OAuthLoginSuccessHandler 실행하여 리프레시 토큰 발급
            MvcResult callbackResult = mockMvc.perform(MockMvcRequestBuilders.get("/login/oauth2/code/google")
                            .param("code", code)
                            .param("state", storedState)
                            .cookie(authCookies))
                    .andReturn();

            // 로그인 성공 후 프론트엔드 콜백 URL로 리다이렉트 검증
            assertThat(callbackResult.getResponse().getStatus()).isEqualTo(302);
            assertThat(callbackResult.getResponse().getHeader(HttpHeaders.LOCATION))
                    .isEqualTo("http://localhost:3000/oauth/callback");

            // 리프레시 토큰이 쿠키로 발급되었는지 검증
            assertThat(callbackResult.getResponse().getHeaders(HttpHeaders.SET_COOKIE))
                    .anyMatch(header -> header.contains("refreshToken="));
        }

        // [5단계] DB 검증: 사용자 및 OAuth 연동 정보가 정상적으로 저장되었는지 확인
        assertThat(memberRepository.count()).isOne();

        Optional<OAuthMember> oauthMember = oAuthMemberRepository.findByProviderAndProviderId("google", "fake-google-sub");
        assertThat(oauthMember).isPresent();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(oauthMember.get().getProvider()).isEqualTo("google");
            softly.assertThat(oauthMember.get().getProviderId()).isEqualTo("fake-google-sub");
            softly.assertThat(oauthMember.get().getMember().getEmail()).isEqualTo("google-user@example.com");
        });
    }
}
