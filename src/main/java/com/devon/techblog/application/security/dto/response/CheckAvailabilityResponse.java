package com.devon.techblog.application.security.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용 가능 여부 응답 DTO")
public record CheckAvailabilityResponse(
        @Schema(description = "사용 가능 여부", example = "true")
        boolean available
) {}
