package com.devon.techblog.application.security.controller;

import com.devon.techblog.application.member.dto.request.SignupRequest;
import com.devon.techblog.application.security.controller.docs.AuthApiDocs;
import com.devon.techblog.application.security.dto.request.LoginRequest;
import com.devon.techblog.application.security.dto.response.CheckAvailabilityResponse;
import com.devon.techblog.application.security.dto.response.LoginResponse;
import com.devon.techblog.application.security.dto.response.RefreshTokenResponse;
import com.devon.techblog.application.security.service.SignupService;
import com.devon.techblog.application.security.service.TokenBlacklistService;
import com.devon.techblog.application.security.service.TokenRefreshService;
import com.devon.techblog.application.security.util.CookieProvider;
import com.devon.techblog.common.dto.api.ApiResponse;
import com.devon.techblog.common.exception.BusinessException;
import com.devon.techblog.common.exception.code.AuthErrorCode;
import com.devon.techblog.domain.member.repository.MemberRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController implements AuthApiDocs {

    private final TokenRefreshService tokenRefreshService;
    private final TokenBlacklistService blacklistService;
    private final CookieProvider cookieProvider;
    private final MemberRepository memberRepository;
    private final SignupService signupService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/signup")
    public ApiResponse<LoginResponse> signUp(
            @RequestBody @Validated SignupRequest request
    ){
        LoginResponse response = signupService.signup(request);
        return ApiResponse.success(response, "signup_success");
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshTokenResponse>> refreshToken(HttpServletRequest request) {
        String refreshToken = cookieProvider.getRefreshTokenFromCookie(request)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND));

        String newAccessToken = tokenRefreshService.refreshAccessToken(refreshToken);

        RefreshTokenResponse response = new RefreshTokenResponse(newAccessToken);
        return ResponseEntity.ok(ApiResponse.success(response, "토큰이 갱신되었습니다"));
    }

    @PostMapping("/blacklist")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> addTokenToBlacklist(
            @RequestBody String token
    ) {
        blacklistService.addToBlacklist(token);

        return ResponseEntity.ok(ApiResponse.success(null, "토큰이 블랙리스트에 등록되었습니다"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {
        throw new UnsupportedOperationException("This endpoint is handled by Spring Security filter");
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        throw new UnsupportedOperationException("This endpoint is handled by Spring Security filter");
    }

    @GetMapping("/check-email")
    public ResponseEntity<ApiResponse<CheckAvailabilityResponse>> checkEmail(
            @RequestParam String email
    ) {
        boolean available = !memberRepository.existsByEmail(email);
        CheckAvailabilityResponse checkResponse = new CheckAvailabilityResponse(available);
        return ResponseEntity.ok(ApiResponse.success(checkResponse, null));
    }

    @GetMapping("/check-nickname")
    public ResponseEntity<ApiResponse<CheckAvailabilityResponse>> checkNickname(
            @RequestParam String nickname
    ) {
        boolean available = !memberRepository.existsByNickname(nickname);
        CheckAvailabilityResponse checkResponse = new CheckAvailabilityResponse(available);
        return ResponseEntity.ok(ApiResponse.success(checkResponse, null));
    }
}
