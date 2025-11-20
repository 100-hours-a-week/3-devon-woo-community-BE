package com.kakaotechbootcamp.community.application.security.config;

import com.kakaotechbootcamp.community.application.security.constants.SecurityConstants;
import com.kakaotechbootcamp.community.application.security.filter.CustomLoginAuthenticationFilter;
import com.kakaotechbootcamp.community.application.security.filter.CustomLogoutFilter;
import com.kakaotechbootcamp.community.application.security.filter.FilterChainExceptionHandler;
import com.kakaotechbootcamp.community.application.security.filter.JwtAuthenticationFilter;
import com.kakaotechbootcamp.community.application.security.handler.CustomAccessDeniedHandler;
import com.kakaotechbootcamp.community.application.security.handler.CustomAuthenticationEntryPoint;
import com.kakaotechbootcamp.community.application.security.handler.LoginFailureHandler;
import com.kakaotechbootcamp.community.application.security.handler.LoginSuccessHandler;
import com.kakaotechbootcamp.community.application.security.handler.OAuthLoginSuccessHandler;
import com.kakaotechbootcamp.community.application.security.repository.HttpCookieOAuth2AuthorizationRequestRepository;
import com.kakaotechbootcamp.community.application.security.service.OAuthLoginService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
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
    private final CustomLogoutFilter customLogoutFilter;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final OAuthLoginSuccessHandler oAuthLoginSuccessHandler;
    private final OAuthLoginService oAuthLoginService;
    private final HttpCookieOAuth2AuthorizationRequestRepository authorizationRequestRepository;

    @Bean
    public AuthenticationManager authenticationManager() throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                /// [CORS 설정]
                .cors(cors -> cors.configurationSource(corsConfigurationSource))

                /// [CSRF 설정] : 비활성화 (Why?: REST API + JWT stateless이므로 불필요) */
                .csrf(AbstractHttpConfigurer::disable)

                /// [Session 캐싱 설정] : 비활성화 (Why?: REST API + JWT stateless JWT access header / refresh cookie)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                /// [Request 캐시 설정] = 비황성화 (Wht?: REST API + JWT stateless이므로 불필요)
                .requestCache(cache -> cache
                        .requestCache(new NullRequestCache())
                )

                /// [Request 권한 설정]
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(SecurityConstants.PUBLIC_URLS).permitAll()
                        .requestMatchers(SecurityConstants.SECURE_URLS).hasRole("USER")
                        .requestMatchers(SecurityConstants.ADMIN_URLS).hasRole("ADMIN")
                        .anyRequest().authenticated()
                )

                .formLogin(AbstractHttpConfigurer::disable)

                ///  [OAuth 로그인 설정]
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(oAuthLoginService)
                        )
                        .authorizationEndpoint(auth -> auth
                                .authorizationRequestRepository(authorizationRequestRepository)
                        )
                        .successHandler(oAuthLoginSuccessHandler)
                )

                /// -> 커스텀 일반 로그인 필터]
                .addFilterAt(
                        createLoginAuthenticationFilter(authenticationManager()),
                        UsernamePasswordAuthenticationFilter.class
                )

                /// JWT 인증 필터 추가
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                )

                /// [커스텀 로그아웃 필터]
                .addFilterBefore(
                        customLogoutFilter,
                        UsernamePasswordAuthenticationFilter.class
                )

                /// [필터 체인 전역 예외 헨들러] : 모든 예외
                .addFilterBefore(
                        filterChainExceptionHandler,
                        UsernamePasswordAuthenticationFilter.class
                )

                /// [Auth 예외 처리 핸들러 설정] : (401, 403)
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )

        ;

        return http.build();
    }

    private CustomLoginAuthenticationFilter createLoginAuthenticationFilter(AuthenticationManager authenticationManager) {
        CustomLoginAuthenticationFilter filter = new CustomLoginAuthenticationFilter(
                authenticationManager, loginSuccessHandler, loginFailureHandler);
        filter.setFilterProcessesUrl(SecurityConstants.LOGIN_URL);
        return filter;
    }
}
