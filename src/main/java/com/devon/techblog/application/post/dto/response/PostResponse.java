package com.devon.techblog.application.post.dto.response;

import com.devon.techblog.application.member.dto.response.MemberResponse;
import com.devon.techblog.domain.member.entity.Member;
import com.devon.techblog.domain.post.entity.Attachment;
import com.devon.techblog.domain.post.entity.Post;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

@Schema(description = "게시글 응답 DTO")
public record PostResponse(
        @Schema(description = "게시글 ID", example = "1")
        Long postId,

        @Schema(description = "작성자 정보")
        MemberResponse member,

        @Schema(description = "게시글 제목", example = "This is a title.")
        String title,

        @Schema(description = "게시글 내용", example = "This is a content.")
        String content,

        @Schema(description = "이미지 URL", example = "https://picsum.photos/200")
        String imageUrl,

        @Schema(description = "생성 시각")
        Instant createdAt,

        @Schema(description = "수정 시각")
        Instant updatedAt,

        @Schema(description = "조회수", example = "100")
        Long viewCount,

        @Schema(description = "좋아요 수", example = "10")
        Long likeCount,

        @Schema(description = "댓글 수", example = "5")
        Long commentCount,

        @Schema(description = "회원의 좋아요 여부", example = "false")
        boolean isLiked,

        @Schema(description = "요약", example = "게시글 요약입니다")
        String summary,

        @Schema(description = "태그 목록", example = "[\"Java\", \"Spring\"]")
        List<String> tags,

        @Schema(description = "시리즈 ID", example = "1")
        Long seriesId,

        @Schema(description = "시리즈 이름", example = "Spring Boot Tutorial")
        String seriesName,

        @Schema(description = "공개 범위", example = "public")
        String visibility
) {
    public static PostResponse of(Post post, Member member, Attachment attachment) {
        return of(post, member, attachment, false);
    }

    public static PostResponse of(Post post, Member member, Attachment attachment, boolean isLiked) {
        return new PostResponse(
                post.getId(),
                MemberResponse.of(member),
                post.getTitle(),
                post.getContent(),
                post.getImageUrl() != null ? post.getImageUrl() : (attachment != null ? attachment.getAttachmentUrl() : null),
                post.getCreatedAt(),
                post.getUpdatedAt(),
                post.getViewsCount(),
                post.getLikeCount(),
                post.getCommentCount(),
                isLiked,
                post.getSummary(),
                post.getTagNames(),
                post.getSeries() != null ? post.getSeries().getId() : null,
                post.getSeries() != null ? post.getSeries().getName() : null,
                post.getVisibility() != null ? post.getVisibility() : "public"
        );
    }
}