package com.kakaotechbootcamp.community.application.post.dto.response;

import com.kakaotechbootcamp.community.application.member.dto.response.MemberResponse;
import com.kakaotechbootcamp.community.domain.member.entity.Member;
import com.kakaotechbootcamp.community.domain.post.entity.Attachment;
import com.kakaotechbootcamp.community.domain.post.entity.Post;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

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
        @Schema(description = "회원의 좋아요 여부", example = "false")
        boolean isLiked
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
                attachment != null ? attachment.getAttachmentUrl() : null,
                post.getCreatedAt(),
                post.getUpdatedAt(),
                post.getViewsCount(),
                post.getLikeCount(),
                isLiked
        );
    }
}