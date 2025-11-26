package com.kakaotechbootcamp.community.application.comment.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "댓글 목록 응답 DTO")
public record CommentListResponse(
        @Schema(description = "게시글 ID", example = "1")
        Long postId,
        @Schema(description = "댓글 목록")
        List<CommentResponse> items,
        @Schema(description = "현재 페이지 번호", example = "0")
        int page,
        @Schema(description = "페이지 크기", example = "10")
        int size,
        @Schema(description = "전체 댓글 수", example = "100")
        long totalElements,
        @Schema(description = "전체 페이지 수", example = "10")
        int totalPages
) {
    public static CommentListResponse of(Long postId, List<CommentResponse> comments, int page, int size) {
        return new CommentListResponse(
                postId,
                comments,
                page,
                size,
                (long) comments.size(),
                (int) Math.ceil((double) comments.size() / size)
        );
    }
}