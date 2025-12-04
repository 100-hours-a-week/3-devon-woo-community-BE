package com.devon.techblog.application.member.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "회원가입 응답 DTO")
public record SignupResponse(
        @Schema(description = "사용자 ID", example = "1")
        Long userId
) {}