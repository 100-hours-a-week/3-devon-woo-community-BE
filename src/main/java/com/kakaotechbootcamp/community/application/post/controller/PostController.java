package com.kakaotechbootcamp.community.application.post.controller;

import com.kakaotechbootcamp.community.application.common.dto.request.PageSortRequest;
import com.kakaotechbootcamp.community.application.common.dto.response.PageResponse;
import com.kakaotechbootcamp.community.application.post.dto.ViewContext;
import com.kakaotechbootcamp.community.application.post.dto.request.PostCreateRequest;
import com.kakaotechbootcamp.community.application.post.dto.request.PostUpdateRequest;
import com.kakaotechbootcamp.community.application.post.dto.response.PostResponse;
import com.kakaotechbootcamp.community.application.post.dto.response.PostSummaryResponse;
import com.kakaotechbootcamp.community.application.post.service.PostLikeService;
import com.kakaotechbootcamp.community.application.post.service.PostService;
import com.kakaotechbootcamp.community.application.post.service.PostViewService;
import com.kakaotechbootcamp.community.application.security.annotation.CurrentUser;
import com.kakaotechbootcamp.community.common.dto.api.ApiResponse;
import com.kakaotechbootcamp.community.common.swagger.CustomExceptionDescription;
import com.kakaotechbootcamp.community.common.swagger.SwaggerResponseDescription;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Post", description = "게시글 관련 API")
@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final PostLikeService postLikeService;
    private final PostViewService postViewService;

    @Operation(summary = "게시글 생성", description = "새로운 게시글을 작성합니다.")
    @CustomExceptionDescription(SwaggerResponseDescription.POST_CREATE)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PostResponse> createPost(
            @RequestBody @Validated PostCreateRequest request,
            @CurrentUser Long memberId
    ) {
        PostResponse response = postService.createPost(request, memberId);
        return ApiResponse.success(response, "post_created");
    }

    @Operation(summary = "게시글 수정", description = "기존 게시글을 수정합니다.")
    @CustomExceptionDescription(SwaggerResponseDescription.POST_UPDATE)
    @PatchMapping("/{postId}")
    public ApiResponse<PostResponse> updatePost(
            @Parameter(description = "게시글 ID") @PathVariable Long postId,
            @RequestBody @Validated PostUpdateRequest request,
            @CurrentUser Long memberId
    ) {
        PostResponse response = postService.updatePost(postId, request, memberId);
        return ApiResponse.success(response, "post_updated");
    }

    @Operation(summary = "게시글 삭제", description = "게시글을 삭제합니다.")
    @CustomExceptionDescription(SwaggerResponseDescription.POST_DELETE)
    @DeleteMapping("/{postId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePost(
            @Parameter(description = "게시글 ID") @PathVariable Long postId,
            @CurrentUser Long memberId
    ) {
        postService.deletePost(postId, memberId);
    }

    @Operation(summary = "게시글 단건 조회", description = "특정 게시글의 상세 정보를 조회합니다.")
    @CustomExceptionDescription(SwaggerResponseDescription.POST_GET)
    @GetMapping("/{postId}")
    public ApiResponse<PostResponse> getPost(
            @Parameter(description = "게시글 ID") @PathVariable Long postId,
            @CurrentUser Long memberId,
            HttpServletRequest httpRequest
    ) {
        PostResponse response = postService.getPostDetails(postId, memberId);

        ViewContext context = ViewContext.from(httpRequest, memberId);
        postViewService.incrementViewCount(postId, context);

        return ApiResponse.success(response, "post_retrieved");
    }

    @Operation(summary = "게시글 목록 조회", description = "게시글 목록을 페이징하여 조회합니다.")
    @CustomExceptionDescription(SwaggerResponseDescription.POST_LIST)
    @GetMapping
    public ApiResponse<PageResponse<PostSummaryResponse>> getPostPage(
            @Parameter(description = "페이지 번호", example = "0")
            @RequestParam(required = false) Integer page,

            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(required = false) Integer size,

            @Parameter(description = "정렬 기준 (필드명,방향). 다중 정렬 가능", example = "createdAt,desc")
            @RequestParam(required = false) List<String> sort
    ) {
        PageSortRequest pageSortRequest = new PageSortRequest(page, size, sort);
        PageResponse<PostSummaryResponse> response = postService.getPostPage(pageSortRequest.toPageable());
        return ApiResponse.success(response, "posts_retrieved");
    }

    @Operation(summary = "게시글 좋아요", description = "게시글에 좋아요를 추가합니다.")
    @CustomExceptionDescription(SwaggerResponseDescription.POST_LIKE)
    @PostMapping("/{postId}/like")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void likePost(
            @Parameter(description = "게시글 ID") @PathVariable Long postId,
            @CurrentUser Long memberId
    ) {
        postLikeService.likePost(postId, memberId);
    }

    @Operation(summary = "게시글 좋아요 취소", description = "게시글의 좋아요를 취소합니다.")
    @CustomExceptionDescription(SwaggerResponseDescription.POST_UNLIKE)
    @DeleteMapping("/{postId}/like")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlikePost(
            @Parameter(description = "게시글 ID") @PathVariable Long postId,
            @CurrentUser Long memberId
    ) {
        postLikeService.unlikePost(postId, memberId);
    }
}