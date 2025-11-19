package com.kakaotechbootcamp.community.application.security.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "로그인 응답 DTO")
public record LoginResponse(
        @Schema(description = "사용자 ID", example = "1")
        Long userId,

        @Schema(description = "새로운 액세스 토큰")
        String accessToken
) {}