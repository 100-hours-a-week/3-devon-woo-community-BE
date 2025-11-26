package com.devon.techblog.application.member.dto.response;

import com.devon.techblog.domain.member.entity.Member;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "회원 정보 응답 DTO")
public record MemberResponse(
        @Schema(description = "회원 ID", example = "1")
        Long memberId,
        @Schema(description = "닉네임", example = "devon")
        String nickname,
        @Schema(description = "프로필 이미지 URL", example = "https://picsum.photos/200")
        String profileImage
) {
    public static MemberResponse of(Member member) {
        return new MemberResponse(
                member.getId(),
                member.getNickname(),
                member.getProfileImageUrl()
        );
    }
}
