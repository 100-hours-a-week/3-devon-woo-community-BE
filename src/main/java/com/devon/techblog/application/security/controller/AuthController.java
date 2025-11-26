package com.devon.techblog.application.security.controller;

import com.devon.techblog.application.security.dto.response.RefreshTokenResponse;
import com.devon.techblog.application.security.service.TokenBlacklistService;
import com.devon.techblog.application.security.service.TokenRefreshService;
import com.devon.techblog.application.security.util.CookieProvider;
import com.devon.techblog.common.dto.api.ApiResponse;
import com.devon.techblog.common.exception.CustomException;
import com.devon.techblog.common.exception.code.AuthErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
