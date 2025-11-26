package com.devon.techblog.application.member.controller;

import com.devon.techblog.application.member.dto.request.SignupRequest;
import com.devon.techblog.application.member.dto.response.SignupResponse;
import com.devon.techblog.application.member.service.SignupService;
import com.devon.techblog.common.dto.api.ApiResponse;
import com.devon.techblog.common.swagger.CustomExceptionDescription;
import com.devon.techblog.common.swagger.SwaggerResponseDescription;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "인증 관련 API")
@RestController
@RequestMapping("auth/signup")
@RequiredArgsConstructor
public class SignupController {

    private final SignupService signupService;

    @Operation(summary = "회원가입", description = "새로운 회원을 등록합니다.")
    @CustomExceptionDescription(SwaggerResponseDescription.AUTH_SIGNUP)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SignupResponse> signUp(
            @RequestBody @Validated SignupRequest request
    ){
        SignupResponse response = signupService.signup(request);
        return ApiResponse.success(response, "signup_success");
    }

}
