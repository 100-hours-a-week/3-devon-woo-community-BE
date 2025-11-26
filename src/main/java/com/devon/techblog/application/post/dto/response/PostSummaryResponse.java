package com.devon.techblog.application.post.dto.response;

import com.devon.techblog.application.member.dto.response.MemberResponse;
import com.devon.techblog.domain.post.dto.PostQueryDto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "게시글 요약 응답 DTO")
public record PostSummaryResponse(
        @Schema(description = "게시글 ID", example = "1")
        Long postId,
        @Schema(description = "게시글 제목", example = "This is a title.")
        String title,
        @Schema(description = "작성자 정보")
        MemberResponse member,
        @Schema(description = "생성 시각")
        Instant createdAt,
        @Schema(description = "조회수", example = "100")
        Long viewCount,
        @Schema(description = "좋아요 수", example = "10")
        Long likeCount,
        @Schema(description = "댓글 수", example = "5")
        Long commentCount
) {
    public static PostSummaryResponse fromDto(PostQueryDto dto) {
        return new PostSummaryResponse(
                dto.postId(),
                dto.title(),
                new MemberResponse(
                        dto.memberId(),
                        dto.memberNickname(),
                        dto.memberProfileImageUrl()
                ),
                dto.createdAt(),
                dto.viewsCount(),
                dto.likeCount(),
                dto.commentCount()
        );
    }
}