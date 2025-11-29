package com.devon.techblog.application.security.controller;

import com.devon.techblog.application.security.dto.request.LoginRequest;
import com.devon.techblog.application.security.dto.response.CheckAvailabilityResponse;
import com.devon.techblog.application.security.dto.response.LoginResponse;
import com.devon.techblog.application.security.dto.response.RefreshTokenResponse;
import com.devon.techblog.application.security.service.TokenBlacklistService;
import com.devon.techblog.application.security.service.TokenRefreshService;
import com.devon.techblog.application.security.util.CookieProvider;
import com.devon.techblog.common.dto.api.ApiResponse;
import com.devon.techblog.common.exception.CustomException;
import com.devon.techblog.common.exception.code.AuthErrorCode;
import com.devon.techblog.domain.member.repository.MemberRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "인증", description = "인증 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final TokenRefreshService tokenRefreshService;
    private final TokenBlacklistService blacklistService;
    private final CookieProvider cookieProvider;
    private final MemberRepository memberRepository;

    @Operation(
            summary = "액세스 토큰 갱신",
            description = "리프레시 토큰을 사용하여 새로운 액세스 토큰을 발급받습니다"
    )
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshTokenResponse>> refreshToken(HttpServletRequest request) {
        String refreshToken = cookieProvider.getRefreshTokenFromCookie(request)
                .orElseThrow(() -> new CustomException(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND));

        String newAccessToken = tokenRefreshService.refreshAccessToken(refreshToken);

        RefreshTokenResponse response = new RefreshTokenResponse(newAccessToken);
        return ResponseEntity.ok(ApiResponse.success(response, "토큰이 갱신되었습니다"));
    }

    @Operation(
            summary = "관리자용 토큰 블랙리스트 등록",
            description = "관리자가 특정 JWT 토큰을 블랙리스트에 등록합니다"
    )
    @PostMapping("/blacklist")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> addTokenToBlacklist(
            @RequestBody String token
    ) {
        blacklistService.addToBlacklist(token);

        return ResponseEntity.ok(ApiResponse.success(null, "토큰이 블랙리스트에 등록되었습니다"));
    }

    @Operation(
            summary = "로그인",
            description = "이메일과 비밀번호로 로그인합니다. "
                    + "실제 처리는 Spring Security Filter에서 처리되며, "
                    + "이 엔드포인트는 Swagger 문서화용입니다."
    )
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {
        throw new UnsupportedOperationException("This endpoint is handled by Spring Security filter");
    }

    @Operation(
            summary = "로그아웃",
            description = "로그아웃하고 리프레시 토큰 쿠키를 무효화합니다. "
                    + "실제 처리는 Spring Security Filter에서 처리되며, "
                    + "이 엔드포인트는 Swagger 문서화용입니다."
    )
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        throw new UnsupportedOperationException("This endpoint is handled by Spring Security filter");
    }

    @Operation(
            summary = "이메일 중복 확인",
            description = "이메일 사용 가능 여부를 확인합니다"
    )
    @GetMapping("/check-email")
    public ResponseEntity<ApiResponse<CheckAvailabilityResponse>> checkEmail(
            @RequestParam String email
    ) {
        boolean available = !memberRepository.existsByEmail(email);
        CheckAvailabilityResponse response = new CheckAvailabilityResponse(available);
        return ResponseEntity.ok(ApiResponse.success(response, null));
    }

    @Operation(
            summary = "닉네임 중복 확인",
            description = "닉네임 사용 가능 여부를 확인합니다"
    )
    @GetMapping("/check-nickname")
    public ResponseEntity<ApiResponse<CheckAvailabilityResponse>> checkNickname(
            @RequestParam String nickname
    ) {
        boolean available = !memberRepository.existsByNickname(nickname);
        CheckAvailabilityResponse response = new CheckAvailabilityResponse(available);
        return ResponseEntity.ok(ApiResponse.success(response, null));
    }
}
