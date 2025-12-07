package com.devon.techblog.application.security.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.devon.techblog.application.security.util.SecurityResponseSender;
import com.devon.techblog.config.annotation.UnitTest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.AuthenticationException;

@UnitTest
class LoginFailureHandlerTest {

    private LoginFailureHandler handler;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        SecurityResponseSender securityResponseSender = new SecurityResponseSender(objectMapper);
        handler = new LoginFailureHandler(securityResponseSender);
    }

    @Test
    @DisplayName("로그인 실패 시 401과 실패 메시지를 반환한다")
    void onAuthenticationFailure_returnsUnauthorized() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AuthenticationException exception = mock(AuthenticationException.class);

        handler.onAuthenticationFailure(request, response, exception);

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(body.get("success").asBoolean()).isFalse();
        assertThat(body.get("message").asText()).isEqualTo("로그인이 실패했습니다");
    }
}
