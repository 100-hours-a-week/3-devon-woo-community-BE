package com.devon.techblog.application.security.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.devon.techblog.application.security.dto.user.CustomUserDetails;
import com.devon.techblog.application.security.service.LoginService;
import com.devon.techblog.application.security.util.JwtTokenProvider;
import com.devon.techblog.config.annotation.UnitTest;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

@UnitTest
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private LoginService loginService;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("유효한 액세스 토큰이면 SecurityContext가 세팅된다")
    void doFilterInternal_setsAuthentication() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        request.addHeader("Authorization", "Bearer valid-token");

        given(jwtTokenProvider.isAccessToken("valid-token")).willReturn(true);
        given(jwtTokenProvider.isTokenExpired("valid-token")).willReturn(false);
        given(jwtTokenProvider.getUidFromToken("valid-token")).willReturn(1L);
        given(jwtTokenProvider.getRoleFromToken("valid-token")).willReturn("USER");

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .isInstanceOf(CustomUserDetails.class);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("토큰 검증 실패 시 필터 체인은 계속 진행되고 인증이 세팅되지 않는다")
    void doFilterInternal_handlesJwtException() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        request.addHeader("Authorization", "Bearer invalid-token");

        given(jwtTokenProvider.isAccessToken("invalid-token")).willThrow(new JwtException("invalid"));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }
}
