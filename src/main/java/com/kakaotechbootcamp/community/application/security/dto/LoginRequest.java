package com.kakaotechbootcamp.community.application.security.dto;

import static com.kakaotechbootcamp.community.common.validation.ValidationMessages.INVALID_EMAIL_FORMAT;
import static com.kakaotechbootcamp.community.common.validation.ValidationMessages.REQUIRED_FIELD;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "로그인 요청 DTO")
public record LoginRequest(

        @Schema(description = "사용자 이메일", example = "devon@email.com")
        @NotBlank(message = REQUIRED_FIELD)
        @Email(message = INVALID_EMAIL_FORMAT)
        String email,

        @Schema(description = "사용자 비밀번호", example = "password1234")
        @NotBlank(message = REQUIRED_FIELD)
        String password
) {}

