package com.devon.techblog.application.comment.controller.docs;

import com.devon.techblog.application.comment.dto.request.CommentCreateRequest;
import com.devon.techblog.application.comment.dto.request.CommentUpdateRequest;
import com.devon.techblog.application.comment.dto.response.CommentResponse;
import com.devon.techblog.application.common.dto.response.PageResponse;
import com.devon.techblog.common.dto.api.ApiResponse;
import com.devon.techblog.common.swagger.CustomErrorResponseDescription;
import com.devon.techblog.common.swagger.SwaggerErrorResponseDescription;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;

@Tag(
        name = "Comment",
        description = "댓글 관련 API"
)
public interface CommentApiDocs {

    @Operation(
            summary = "댓글 생성",
            description = "게시글에 새로운 댓글을 작성합니다."
    )
    @CustomErrorResponseDescription(SwaggerErrorResponseDescription.COMMENT_CREATE)
    ApiResponse<CommentResponse> createComment(
            @Parameter(description = "게시글 ID") Long postId,
            CommentCreateRequest request,
            Long memberId
    );

    @Operation(
            summary = "게시글의 댓글 목록 조회",
            description = "특정 게시글의 댓글 목록을 페이징하여 조회합니다."
    )
    @CustomErrorResponseDescription(SwaggerErrorResponseDescription.COMMENT_LIST)
    ApiResponse<PageResponse<CommentResponse>> getCommentPage(
            @Parameter(description = "게시글 ID") Long postId,
            @Parameter(description = "페이지 번호", example = "0") Integer page,
            @Parameter(description = "페이지 크기", example = "10") Integer size,
            @Parameter(description = "정렬 기준 (필드명,방향). 다중 정렬 가능", example = "createdAt,asc") List<String> sort
    );

    @Operation(
            summary = "댓글 단건 조회",
            description = "특정 댓글의 상세 정보를 조회합니다."
    )
    @CustomErrorResponseDescription(SwaggerErrorResponseDescription.COMMENT_GET)
    ApiResponse<CommentResponse> getComment(
            @Parameter(description = "댓글 ID") Long commentId
    );

    @Operation(
            summary = "댓글 수정",
            description = "기존 댓글을 수정합니다."
    )
    @CustomErrorResponseDescription(SwaggerErrorResponseDescription.COMMENT_UPDATE)
    ApiResponse<CommentResponse> updateComment(
            @Parameter(description = "댓글 ID") Long commentId,
            CommentUpdateRequest request,
            Long memberId
    );

    @Operation(
            summary = "댓글 삭제",
            description = "댓글을 삭제합니다."
    )
    @CustomErrorResponseDescription(SwaggerErrorResponseDescription.COMMENT_DELETE)
    void deleteComment(
            @Parameter(description = "댓글 ID") Long commentId,
            Long memberId
    );
}
