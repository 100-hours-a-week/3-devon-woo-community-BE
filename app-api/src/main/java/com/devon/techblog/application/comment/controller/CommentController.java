package com.devon.techblog.application.comment.controller;

import com.devon.techblog.application.comment.controller.docs.CommentApiDocs;
import com.devon.techblog.application.comment.dto.request.CommentCreateRequest;
import com.devon.techblog.application.comment.dto.request.CommentUpdateRequest;
import com.devon.techblog.application.comment.dto.response.CommentResponse;
import com.devon.techblog.application.comment.service.CommentService;
import com.devon.techblog.application.common.dto.request.PageSortRequest;
import com.devon.techblog.application.common.dto.response.PageResponse;
import com.devon.techblog.application.security.annotation.CurrentUser;
import com.devon.techblog.common.dto.api.ApiResponse;
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

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CommentController implements CommentApiDocs {

    private final CommentService commentService;

    @PostMapping("/posts/{postId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CommentResponse> createComment(
            @PathVariable Long postId,
            @RequestBody @Validated CommentCreateRequest request,
            @CurrentUser Long memberId
    ) {
        CommentResponse response = commentService.createComment(postId, request, memberId);
        return ApiResponse.success(response, "comment_created");
    }

    @GetMapping("/posts/{postId}/comments")
    public ApiResponse<PageResponse<CommentResponse>> getCommentPage(
            @PathVariable Long postId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) List<String> sort
    ) {
        PageSortRequest pageSortRequest = new PageSortRequest(page, size, sort);
        PageResponse<CommentResponse> response = commentService.getCommentPageByPostId(postId, pageSortRequest.toPageable());
        return ApiResponse.success(response, "comments_retrieved");
    }

    @GetMapping("/comments/{commentId}")
    public ApiResponse<CommentResponse> getComment(
            @PathVariable Long commentId
    ) {
        CommentResponse response = commentService.getCommentsDetails(commentId);
        return ApiResponse.success(response, "comment_fetched");
    }

    @PatchMapping("/comments/{commentId}")
    public ApiResponse<CommentResponse> updateComment(
            @PathVariable Long commentId,
            @RequestBody @Validated CommentUpdateRequest request,
            @CurrentUser Long memberId
    ) {
        CommentResponse response = commentService.updateComment(commentId, request, memberId);
        return ApiResponse.success(response, "comment_updated");
    }

    @DeleteMapping("/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(
            @PathVariable Long commentId,
            @CurrentUser Long memberId
    ) {
        commentService.deleteComment(commentId, memberId);
    }
}
