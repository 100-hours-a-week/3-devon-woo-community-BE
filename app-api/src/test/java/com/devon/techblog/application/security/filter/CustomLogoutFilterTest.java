package com.devon.techblog.application.security.filter;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.devon.techblog.application.security.constants.SecurityConstants;
import com.devon.techblog.application.security.handler.LogoutHandler;
import com.devon.techblog.config.annotation.UnitTest;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@UnitTest
class CustomLogoutFilterTest {

    @Mock
    private LogoutHandler logoutHandler;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private CustomLogoutFilter customLogoutFilter;

    @Test
    @DisplayName("POST /auth/logout 요청이면 LogoutHandler를 호출하고 FilterChain을 건너뛴다")
    void doFilterInternal_handlesLogoutRequest() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(SecurityConstants.LOGOUT_URL);
        request.setMethod("POST");
        MockHttpServletResponse response = new MockHttpServletResponse();

        customLogoutFilter.doFilterInternal(request, response, filterChain);

        verify(logoutHandler).onLogout(request, response);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("다른 URL이나 메서드는 FilterChain을 계속 호출한다")
    void doFilterInternal_passesThroughNonLogoutRequest() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/posts");
        request.setMethod("GET");
        MockHttpServletResponse response = new MockHttpServletResponse();

        customLogoutFilter.doFilterInternal(request, response, filterChain);

        verify(logoutHandler, never()).onLogout(request, response);
        verify(filterChain).doFilter(request, response);
    }
}
