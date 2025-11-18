package com.kakaotechbootcamp.community.application.security.config;

import com.kakaotechbootcamp.community.application.security.constants.SecurityConstants;
import com.kakaotechbootcamp.community.application.security.filter.FilterChainExceptionHandler;
import com.kakaotechbootcamp.community.application.security.filter.LoginAuthenticationFilter;
import com.kakaotechbootcamp.community.application.security.handler.CustomAccessDeniedHandler;
import com.kakaotechbootcamp.community.application.security.handler.CustomAuthenticationEntryPoint;
import com.kakaotechbootcamp.community.application.security.handler.LoginFailureHandler;
import com.kakaotechbootcamp.community.application.security.handler.LoginSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.savedrequest.NullRequestCache;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final AuthenticationConfiguration authenticationConfiguration;
    private final LoginSuccessHandler loginSuccessHandler;
    private final LoginFailureHandler loginFailureHandler;
    private final CorsConfigurationSource corsConfigurationSource;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;
    private final FilterChainExceptionHandler filterChainExceptionHandler;

    @Bean
    public AuthenticationManager authenticationManager() throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                /* CORS 설정 */
                .cors(cors -> cors.configurationSource(corsConfigurationSource))

                /* CSRF 보호 = 비활성화 (Why?: REST API + JWT stateless이므로 불필요) */
                .csrf(AbstractHttpConfigurer::disable)

                /* 세션 설정 = 비활성화 (Why?: REST API + JWT stateless이므로 불필요) */
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                /* 요청 캐시 = 비황성화 (Wht?: REST API + JWT stateless이므로 불필요)*/
                .requestCache(cache -> cache
                        .requestCache(new NullRequestCache())
                )

                /* 요청 권한 설정 */
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(SecurityConstants.PUBLIC_URLS).permitAll()
                        .requestMatchers(SecurityConstants.SECURE_URLS).hasRole("USER")
                        .requestMatchers(SecurityConstants.ADMIN_URLS).hasRole("ADMIN")
                        .anyRequest().authenticated()
                )

                /* 커스텀 로그인 필터 추가 (Bean으로 등록하지 않고 직접 생성) */
                .addFilterAt(
                        createLoginAuthenticationFilter(authenticationManager()),
                        UsernamePasswordAuthenticationFilter.class
                )

                /* Auth 예외 처리 핸들러 설정 : (401, 403) */
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )

                /* 필터 체인 전역 예외 처리 핸들러 : 모든 예외 (모든 필터보다 먼저 실행) */
                .addFilterBefore(
                        filterChainExceptionHandler, UsernamePasswordAuthenticationFilter.class
                )

        ;

        return http.build();
    }

    private LoginAuthenticationFilter createLoginAuthenticationFilter(AuthenticationManager authenticationManager) {
        LoginAuthenticationFilter filter = new LoginAuthenticationFilter(
                authenticationManager, loginSuccessHandler, loginFailureHandler);
        filter.setFilterProcessesUrl(SecurityConstants.LOGIN_URL);
        return filter;
    }
}
