package com.kakaotechbootcamp.community.application.member.dto.request;

import static com.kakaotechbootcamp.community.common.validation.ValidationMessages.INVALID_NICKNAME;
import static com.kakaotechbootcamp.community.common.validation.ValidationMessages.INVALID_PROFILE_IMAGE;
import static com.kakaotechbootcamp.community.common.validation.ValidationPatterns.NICKNAME_MAX_LENGTH;
import static com.kakaotechbootcamp.community.common.validation.ValidationPatterns.URL_PATTERN;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "회원 정보 수정 요청 DTO")
public record MemberUpdateRequest(
        @Schema(description = "새 닉네임", example = "new_devon")
        @Size(max = NICKNAME_MAX_LENGTH, message = INVALID_NICKNAME)
        String nickname,

        @Schema(description = "새 프로필 이미지 URL", example = "https://picsum.photos/300")
        @Pattern(regexp = URL_PATTERN, message = INVALID_PROFILE_IMAGE)
        String profileImage
) {}
