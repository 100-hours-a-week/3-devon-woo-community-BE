package com.devon.techblog.application.post.controller;

import com.devon.techblog.application.common.dto.request.PageSortRequest;
import com.devon.techblog.application.common.dto.response.PageResponse;
import com.devon.techblog.application.post.controller.docs.PostApiDocs;
import com.devon.techblog.application.post.dto.ViewContext;
import com.devon.techblog.application.post.dto.request.PostCreateRequest;
import com.devon.techblog.application.post.dto.request.PostUpdateRequest;
import com.devon.techblog.application.post.dto.response.PostResponse;
import com.devon.techblog.application.post.dto.response.PostSummaryResponse;
import com.devon.techblog.application.post.service.PostLikeService;
import com.devon.techblog.application.post.service.PostService;
import com.devon.techblog.application.post.service.PostViewService;
import com.devon.techblog.application.security.annotation.CurrentUser;
import com.devon.techblog.common.dto.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
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
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController implements PostApiDocs {

    private final PostService postService;
    private final PostLikeService postLikeService;
    private final PostViewService postViewService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PostResponse> createPost(
            @RequestBody @Validated PostCreateRequest request,
            @CurrentUser Long memberId
    ) {
        PostResponse response = postService.createPost(request, memberId);
        return ApiResponse.success(response);
    }

    @PatchMapping("/{postId}")
    public ApiResponse<PostResponse> updatePost(
            @PathVariable Long postId,
            @RequestBody @Validated PostUpdateRequest request,
            @CurrentUser Long memberId
    ) {
        PostResponse response = postService.updatePost(postId, request, memberId);
        return ApiResponse.success(response);
    }

    @DeleteMapping("/{postId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePost(
            @PathVariable Long postId,
            @CurrentUser Long memberId
    ) {
        postService.deletePost(postId, memberId);
    }

    @GetMapping("/{postId}")
    public ApiResponse<PostResponse> getPost(
            @PathVariable Long postId,
            @CurrentUser Long memberId,
            HttpServletRequest httpRequest
    ) {
        PostResponse response = postService.getPostDetails(postId, memberId);

        ViewContext context = ViewContext.from(httpRequest, memberId);
        postViewService.incrementViewCount(postId, context);

        return ApiResponse.success(response);
    }

    @GetMapping
    public ApiResponse<PageResponse<PostSummaryResponse>> getPostPage(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) List<String> sort,
            @RequestParam(required = false) List<String> tags
    ) {
        PageSortRequest pageSortRequest = new PageSortRequest(page, size, sort);
        PageResponse<PostSummaryResponse> response;

        if (tags != null && !tags.isEmpty()) {
            response = postService.getPostPageByTags(tags, pageSortRequest.toPageable());
        } else {
            response = postService.getPostPage(pageSortRequest.toPageable());
        }

        return ApiResponse.success(response);
    }

    @PostMapping("/{postId}/like")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void likePost(
            @PathVariable Long postId,
            @CurrentUser Long memberId
    ) {
        postLikeService.likePost(postId, memberId);
    }

    @DeleteMapping("/{postId}/like")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlikePost(
            @PathVariable Long postId,
            @CurrentUser Long memberId
    ) {
        postLikeService.unlikePost(postId, memberId);
    }
}
