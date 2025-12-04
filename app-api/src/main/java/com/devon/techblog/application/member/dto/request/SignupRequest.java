package com.devon.techblog.application.member.dto.request;

import static com.devon.techblog.common.validation.ValidationMessages.INVALID_EMAIL_FORMAT;
import static com.devon.techblog.common.validation.ValidationMessages.INVALID_NICKNAME;
import static com.devon.techblog.common.validation.ValidationMessages.INVALID_PASSWORD_FORMAT;
import static com.devon.techblog.common.validation.ValidationMessages.INVALID_PROFILE_IMAGE;
import static com.devon.techblog.common.validation.ValidationMessages.REQUIRED_FIELD;
import static com.devon.techblog.common.validation.ValidationPatterns.NICKNAME_MAX_LENGTH;
import static com.devon.techblog.common.validation.ValidationPatterns.PASSWORD_MIN_LENGTH;
import static com.devon.techblog.common.validation.ValidationPatterns.URL_PATTERN;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "회원가입 요청 DTO")
public record SignupRequest(
        @Schema(description = "사용자 이메일", example = "devon@email.com")
        @NotBlank(message = REQUIRED_FIELD)
        @Email(message = INVALID_EMAIL_FORMAT)
        String email,

        @Schema(description = "사용자 비밀번호", example = "password1234")
        @NotBlank(message = REQUIRED_FIELD)
        @Size(min = PASSWORD_MIN_LENGTH, message = INVALID_PASSWORD_FORMAT)
        String password,

        @Schema(description = "사용자 닉네임", example = "devon")
        @NotBlank(message = REQUIRED_FIELD)
        @Size(max = NICKNAME_MAX_LENGTH, message = INVALID_NICKNAME)
        String nickname,

        @Schema(description = "프로필 이미지 URL", example = "https://picsum.photos/200")
        @Pattern(regexp = URL_PATTERN, message = INVALID_PROFILE_IMAGE)
        String profileImage
) {}