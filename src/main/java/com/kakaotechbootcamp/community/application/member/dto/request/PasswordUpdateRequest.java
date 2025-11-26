package com.kakaotechbootcamp.community.application.member.dto.request;

import static com.kakaotechbootcamp.community.common.validation.ValidationMessages.INVALID_PASSWORD_FORMAT;
import static com.kakaotechbootcamp.community.common.validation.ValidationMessages.REQUIRED_FIELD;
import static com.kakaotechbootcamp.community.common.validation.ValidationMessages.REQUIRED_PASSWORD;
import static com.kakaotechbootcamp.community.common.validation.ValidationPatterns.PASSWORD_MIN_LENGTH;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "비밀번호 변경 요청 DTO")
public record PasswordUpdateRequest(
        @Schema(description = "현재 비밀번호", example = "password1234")
        @NotBlank(message = REQUIRED_PASSWORD)
        String currentPassword,

        @Schema(description = "새 비밀번호", example = "new_password1234")
        @NotBlank(message = REQUIRED_FIELD)
        @Size(min = PASSWORD_MIN_LENGTH, message = INVALID_PASSWORD_FORMAT)
        String newPassword
) {}