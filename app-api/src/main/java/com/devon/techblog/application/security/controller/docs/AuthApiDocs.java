package com.devon.techblog.application.security.controller.docs;

import com.devon.techblog.application.member.dto.request.SignupRequest;
import com.devon.techblog.application.security.dto.request.LoginRequest;
import com.devon.techblog.application.security.dto.response.CheckAvailabilityResponse;
import com.devon.techblog.application.security.dto.response.LoginResponse;
import com.devon.techblog.application.security.dto.response.RefreshTokenResponse;
import com.devon.techblog.common.dto.api.ApiResponse;
import com.devon.techblog.common.swagger.CustomErrorResponseDescription;
import com.devon.techblog.common.swagger.SwaggerErrorResponseDescription;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;

@Tag(
        name = "Auth",
        description = "인증 관련 API"
)
public interface AuthApiDocs {

    @Operation(
            summary = "회원가입",
            description = "새로운 회원을 등록하고 액세스 토큰을 발급합니다."
    )
    @CustomErrorResponseDescription(SwaggerErrorResponseDescription.AUTH_SIGNUP)
    ApiResponse<LoginResponse> signUp(
            SignupRequest request
    );

    @Operation(
            summary = "액세스 토큰 갱신",
            description = "리프레시 토큰을 사용하여 새로운 액세스 토큰을 발급받습니다."
    )
    ResponseEntity<ApiResponse<RefreshTokenResponse>> refreshToken(
            HttpServletRequest request
    );

    @Operation(
            summary = "관리자용 토큰 블랙리스트 등록",
            description = "관리자가 특정 JWT 토큰을 블랙리스트에 등록합니다."
    )
    ResponseEntity<ApiResponse<Void>> addTokenToBlacklist(
            String token
    );

    @Operation(
            summary = "로그인",
            description = "이메일과 비밀번호로 로그인합니다. "
                    + "실제 처리는 Spring Security Filter에서 처리되며, "
                    + "이 엔드포인트는 Swagger 문서화용입니다."
    )
    ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid LoginRequest request
    );

    @Operation(
            summary = "로그아웃",
            description = "로그아웃하고 리프레시 토큰 쿠키를 무효화합니다. "
                    + "실제 처리는 Spring Security Filter에서 처리되며, "
                    + "이 엔드포인트는 Swagger 문서화용입니다."
    )
    ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request,
            HttpServletResponse response
    );

    @Operation(
            summary = "이메일 중복 확인",
            description = "이메일 사용 가능 여부를 확인합니다."
    )
    ResponseEntity<ApiResponse<CheckAvailabilityResponse>> checkEmail(
            String email
    );

    @Operation(
            summary = "닉네임 중복 확인",
            description = "닉네임 사용 가능 여부를 확인합니다."
    )
    ResponseEntity<ApiResponse<CheckAvailabilityResponse>> checkNickname(
            String nickname
    );
}

