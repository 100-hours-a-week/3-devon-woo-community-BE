package com.devon.techblog.application.comment.dto.response;

import com.devon.techblog.application.member.dto.response.MemberResponse;
import com.devon.techblog.domain.comment.dto.CommentQueryDto;
import com.devon.techblog.domain.comment.entity.Comment;
import com.devon.techblog.domain.member.entity.Member;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "댓글 응답 DTO")
public record CommentResponse(
        @Schema(description = "댓글 ID", example = "1")
        Long commentId,
        @Schema(description = "게시글 ID", example = "1")
        Long postId,
        @Schema(description = "댓글 내용", example = "This is a comment.")
        String content,
        @Schema(description = "작성자 정보")
        MemberResponse member,
        @Schema(description = "생성 시각")
        Instant createdAt,
        @Schema(description = "수정 시각")
        Instant updatedAt
) {
    public static CommentResponse of(Comment comment, Member member) {
        return new CommentResponse(
                comment.getId(),
                comment.getPost().getId(),
                comment.getContent(),
                MemberResponse.of(member),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }

    public static CommentResponse of(CommentQueryDto dto) {
        return new CommentResponse(
                dto.commentId(),
                dto.postId(),
                dto.content(),
                new MemberResponse(
                        dto.memberId(),
                        dto.memberNickname(),
                        dto.memberProfileImage()
                ),
                dto.createdAt(),
                dto.updatedAt()
        );
    }
}