package com.devon.techblog.domain.comment.dto;

import java.time.Instant;

/**
 * QueryDSL Projection용 댓글 요약 DTO
 * 필요한 필드만 조회하여 N+1 문제 해결 및 성능 최적화
 */
public record CommentQueryDto(
        Long commentId,
        Long postId,
        String content,
        Instant createdAt,
        Instant updatedAt,
        Long memberId,
        String memberNickname,
        String memberProfileImage
) {
}
