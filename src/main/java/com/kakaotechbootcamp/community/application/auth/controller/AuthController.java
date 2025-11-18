package com.kakaotechbootcamp.community.application.auth.controller;

import com.kakaotechbootcamp.community.application.auth.dto.LoginRequest;
import com.kakaotechbootcamp.community.application.auth.dto.LoginResponse;
import com.kakaotechbootcamp.community.application.auth.dto.SignupRequest;
import com.kakaotechbootcamp.community.application.auth.dto.SignupResponse;
import com.kakaotechbootcamp.community.application.auth.service.AuthService;
import com.kakaotechbootcamp.community.common.dto.api.ApiResponse;
import com.kakaotechbootcamp.community.common.swagger.CustomExceptionDescription;
import com.kakaotechbootcamp.community.common.swagger.SwaggerResponseDescription;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "인증 관련 API")
@RestController
@RequestMapping("auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "회원가입", description = "새로운 회원을 등록합니다.")
    @CustomExceptionDescription(SwaggerResponseDescription.AUTH_SIGNUP)
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SignupResponse> signUp(
            @RequestBody @Validated SignupRequest request
    ){
        SignupResponse response = authService.signup(request);
        return ApiResponse.success(response, "signup_success");
    }

}
