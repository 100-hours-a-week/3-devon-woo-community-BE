package com.devon.techblog.application.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.devon.techblog.application.security.dto.request.LoginRequest;
import com.devon.techblog.application.security.handler.LoginFailureHandler;
import com.devon.techblog.application.security.handler.LoginSuccessHandler;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@RequiredArgsConstructor
public class CustomLoginAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private final AuthenticationManager authenticationManager;
    private final LoginSuccessHandler successHandler;
    private final LoginFailureHandler failureHandler;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        try {
            String requestBody = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            LoginRequest loginRequest = objectMapper.readValue(requestBody, LoginRequest.class);

            String username = loginRequest.email();
            String password = loginRequest.password();

            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                    username, password);

            return authenticationManager.authenticate(authenticationToken);

        } catch (AuthenticationException e) {
            throw e;
        } catch (IOException e) {
            throw new AuthenticationServiceException("로그인 요청 파싱에 실패했습니다", e);
        } catch (Exception e) {
            throw new AuthenticationServiceException("인증 처리 중 오류가 발생했습니다", e);
        }
    }


    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authentication) throws IOException, ServletException {
        successHandler.onAuthenticationSuccess(request, response, authentication);
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException, ServletException {
        failureHandler.onAuthenticationFailure(request, response, failed);
    }


}
