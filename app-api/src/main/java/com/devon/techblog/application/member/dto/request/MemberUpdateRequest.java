package com.devon.techblog.application.member.dto.request;

import static com.devon.techblog.common.validation.ValidationMessages.INVALID_NICKNAME;
import static com.devon.techblog.common.validation.ValidationMessages.INVALID_PROFILE_IMAGE;
import static com.devon.techblog.common.validation.ValidationPatterns.NICKNAME_MAX_LENGTH;
import static com.devon.techblog.common.validation.ValidationPatterns.URL_PATTERN;

import com.devon.techblog.application.member.dto.SocialLinks;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "회원 정보 수정 요청 DTO")
public record MemberUpdateRequest(
        @Schema(description = "새 닉네임", example = "new_devon")
        @Size(max = NICKNAME_MAX_LENGTH, message = INVALID_NICKNAME)
        String nickname,

        @Schema(description = "새 프로필 이미지 URL", example = "https://picsum.photos/300")
        @Pattern(regexp = URL_PATTERN, message = INVALID_PROFILE_IMAGE)
        String profileImage,

        @Schema(description = "핸들", example = "@newdevon")
        String handle,

        @Schema(description = "자기소개", example = "Backend Developer")
        String bio,

        @Schema(description = "회사", example = "New Company")
        String company,

        @Schema(description = "위치", example = "Busan, Korea")
        String location,

        @Schema(description = "주요 기술 스택", example = "[\"Kotlin\", \"Spring Boot\"]")
        List<String> primaryStack,

        @Schema(description = "관심사", example = "[\"Microservices\", \"Kubernetes\"]")
        List<String> interests,

        @Schema(description = "소셜 링크")
        SocialLinks socialLinks
) {}
