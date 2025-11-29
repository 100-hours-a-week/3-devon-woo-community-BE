package com.devon.techblog.application.member.dto.response;

import com.devon.techblog.application.member.dto.SocialLinks;
import com.devon.techblog.domain.member.entity.Member;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Collections;
import java.util.List;

@Schema(description = "회원 상세 정보 응답 DTO")
public record MemberDetailsResponse(
        @Schema(description = "회원 ID", example = "1")
        Long memberId,

        @Schema(description = "닉네임", example = "devon")
        String nickname,

        @Schema(description = "이메일", example = "email@example.com")
        String email,

        @Schema(description = "프로필 이미지 URL", example = "https://picsum.photos/200")
        String profileImage,

        @Schema(description = "역할", example = "USER")
        String role,

        @Schema(description = "핸들", example = "@devon")
        String handle,

        @Schema(description = "자기소개", example = "안녕하세요")
        String bio,

        @Schema(description = "회사", example = "Tech Company")
        String company,

        @Schema(description = "위치", example = "Seoul, Korea")
        String location,

        @Schema(description = "주요 기술 스택", example = "[\"Java\", \"Spring\", \"React\"]")
        List<String> primaryStack,

        @Schema(description = "관심사", example = "[\"AI\", \"Cloud\", \"DevOps\"]")
        List<String> interests,

        @Schema(description = "소셜 링크")
        SocialLinks socialLinks
) {
    public static MemberDetailsResponse of(Member member) {
        return new MemberDetailsResponse(
                member.getId(),
                member.getNickname(),
                member.getEmail(),
                member.getProfileImageUrl(),
                member.getRole().name(),
                null,
                null,
                null,
                null,
                Collections.emptyList(),
                Collections.emptyList(),
                SocialLinks.empty()
        );
    }
}