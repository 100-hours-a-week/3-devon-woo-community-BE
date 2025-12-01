package com.devon.techblog.application.security.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;

import com.devon.techblog.common.dto.api.ApiResponse;
import com.devon.techblog.config.annotation.UnitTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import javax.security.sasl.AuthenticationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@UnitTest
class FilterChainExceptionFilterTest {

    private final FilterChainExceptionFilter filter =
            new FilterChainExceptionFilter(new ObjectMapper());

    private final HttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @Test
    @DisplayName("Unexpected 예외는 500과 ApiResponse 메시지를 반환한다")
    void doFilterInternal_handlesUnexpectedException() throws ServletException, IOException {
        FilterChain filterChain = mock(FilterChain.class);

        doThrow(new NullPointerException("boom")).when(filterChain).doFilter(request, response);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        assertThat(response.getContentAsString()).contains("서버 오류가 발생했습니다");
        ApiResponse<?> settled = new ObjectMapper().readValue(response.getContentAsString(), ApiResponse.class);
        assertThat(settled.isSuccess()).isFalse();
    }

    @Test
    @DisplayName("AuthenticationException은 그대로 던진다")
    void doFilterInternal_rethrowsAuthenticationException() throws Exception {
        FilterChain filterChain = mock(FilterChain.class);

        doThrow(new AuthenticationException("auth") {}).when(filterChain).doFilter(request, response);

        assertThatThrownBy(() -> filter.doFilterInternal(request, response, filterChain))
                .isInstanceOf(AuthenticationException.class);
    }

    @Test
    @DisplayName("AccessDeniedException도 그대로 던진다")
    void doFilterInternal_rethrowsAccessDeniedException() throws Exception {
        FilterChain filterChain = mock(FilterChain.class);

        doThrow(new AccessDeniedException("denied")).when(filterChain).doFilter(request, response);

        assertThatThrownBy(() -> filter.doFilterInternal(request, response, filterChain))
                .isInstanceOf(AccessDeniedException.class);
    }
}
