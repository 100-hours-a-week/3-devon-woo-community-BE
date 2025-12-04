package com.devon.techblog.application.comment.controller;

import com.devon.techblog.application.comment.dto.request.CommentCreateRequest;
import com.devon.techblog.application.comment.dto.request.CommentUpdateRequest;
import com.devon.techblog.application.comment.dto.response.CommentResponse;
import com.devon.techblog.application.comment.service.CommentService;
import com.devon.techblog.application.common.dto.request.PageSortRequest;
import com.devon.techblog.application.common.dto.response.PageResponse;
import com.devon.techblog.application.security.annotation.CurrentUser;
import com.devon.techblog.common.dto.api.ApiResponse;
import com.devon.techblog.common.swagger.CustomExceptionDescription;
import com.devon.techblog.common.swagger.SwaggerResponseDescription;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Comment", description = "댓글 관련 API")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @Operation(summary = "댓글 생성", description = "게시글에 새로운 댓글을 작성합니다.")
    @CustomExceptionDescription(SwaggerResponseDescription.COMMENT_CREATE)
    @PostMapping("/posts/{postId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CommentResponse> createComment(
            @Parameter(description = "게시글 ID") @PathVariable Long postId,
            @RequestBody @Validated CommentCreateRequest request,
            @CurrentUser Long memberId
    ) {
        CommentResponse response = commentService.createComment(postId, request, memberId);
        return ApiResponse.success(response, "comment_created");
    }

    @Operation(summary = "게시글의 댓글 목록 조회", description = "특정 게시글의 댓글 목록을 페이징하여 조회합니다.")
    @CustomExceptionDescription(SwaggerResponseDescription.COMMENT_LIST)
    @GetMapping("/posts/{postId}/comments")
    public ApiResponse<PageResponse<CommentResponse>> getCommentPage(
            @Parameter(description = "게시글 ID") @PathVariable Long postId,
            @Parameter(description = "페이지 번호", example = "0")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "페이지 크기", example = "10")
            @RequestParam(required = false) Integer size,
            @Parameter(description = "정렬 기준 (필드명,방향). 다중 정렬 가능", example = "createdAt,asc")
            @RequestParam(required = false) List<String> sort
    ) {
        PageSortRequest pageSortRequest = new PageSortRequest(page, size, sort);
        PageResponse<CommentResponse> response = commentService.getCommentPageByPostId(postId, pageSortRequest.toPageable());
        return ApiResponse.success(response, "comments_retrieved");
    }

    @Operation(summary = "댓글 단건 조회", description = "특정 댓글의 상세 정보를 조회합니다.")
    @CustomExceptionDescription(SwaggerResponseDescription.COMMENT_GET)
    @GetMapping("/comments/{commentId}")
    public ApiResponse<CommentResponse> getComment(
            @Parameter(description = "댓글 ID") @PathVariable Long commentId
    ) {
        CommentResponse response = commentService.getCommentsDetails(commentId);
        return ApiResponse.success(response, "comment_fetched");
    }

    @Operation(summary = "댓글 수정", description = "기존 댓글을 수정합니다.")
    @CustomExceptionDescription(SwaggerResponseDescription.COMMENT_UPDATE)
    @PatchMapping("/comments/{commentId}")
    public ApiResponse<CommentResponse> updateComment(
            @Parameter(description = "댓글 ID") @PathVariable Long commentId,
            @RequestBody @Validated CommentUpdateRequest request,
            @CurrentUser Long memberId
    ) {
        CommentResponse response = commentService.updateComment(commentId, request, memberId);
        return ApiResponse.success(response, "comment_updated");
    }

    @Operation(summary = "댓글 삭제", description = "댓글을 삭제합니다.")
    @CustomExceptionDescription(SwaggerResponseDescription.COMMENT_DELETE)
    @DeleteMapping("/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(
            @Parameter(description = "댓글 ID") @PathVariable Long commentId,
            @CurrentUser Long memberId
    ) {
        commentService.deleteComment(commentId, memberId);
    }
}