package com.devon.techblog.application.member.dto.response;

import com.devon.techblog.domain.member.entity.Member;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "회원 상세 정보 응답 DTO")
public record MemberDetailsResponse(
        @Schema(description = "회원 ID", example = "1")
        Long memberId,
        @Schema(description = "닉네임", example = "devon")
        String nickname,
        @Schema(description = "이메일", example = "email")
        String email,
        @Schema(description = "프로필 이미지 URL", example = "https://picsum.photos/200")
        String profileImage
) {
    public static MemberDetailsResponse of(Member member) {
        return new MemberDetailsResponse(
                member.getId(),
                member.getNickname(),
                member.getEmail(),
                member.getProfileImageUrl()
        );
    }
}