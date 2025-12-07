package com.devon.techblog.application.post.controller.docs;

import com.devon.techblog.application.common.dto.response.PageResponse;
import com.devon.techblog.application.post.dto.request.PostCreateRequest;
import com.devon.techblog.application.post.dto.request.PostUpdateRequest;
import com.devon.techblog.application.post.dto.response.PostResponse;
import com.devon.techblog.application.post.dto.response.PostSummaryResponse;
import com.devon.techblog.common.dto.api.ApiResponse;
import com.devon.techblog.common.swagger.CustomErrorResponseDescription;
import com.devon.techblog.common.swagger.SwaggerErrorResponseDescription;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@Tag(
        name = "Post",
        description = "게시글 관련 API"
)
public interface PostApiDocs {

    @Operation(
            summary = "게시글 생성",
            description = "새로운 게시글을 작성합니다."
    )
    @CustomErrorResponseDescription(SwaggerErrorResponseDescription.POST_CREATE)
    ApiResponse<PostResponse> createPost(
            PostCreateRequest request,
            Long memberId
    );

    @Operation(
            summary = "게시글 수정",
            description = "기존 게시글을 수정합니다."
    )
    @CustomErrorResponseDescription(SwaggerErrorResponseDescription.POST_UPDATE)
    ApiResponse<PostResponse> updatePost(
            @Parameter(description = "게시글 ID") Long postId,
            PostUpdateRequest request,
            Long memberId
    );

    @Operation(
            summary = "게시글 삭제",
            description = "게시글을 삭제합니다."
    )
    @CustomErrorResponseDescription(SwaggerErrorResponseDescription.POST_DELETE)
    void deletePost(
            @Parameter(description = "게시글 ID") Long postId,
            Long memberId
    );

    @Operation(
            summary = "게시글 단건 조회",
            description = "특정 게시글의 상세 정보를 조회합니다."
    )
    @CustomErrorResponseDescription(SwaggerErrorResponseDescription.POST_GET)
    ApiResponse<PostResponse> getPost(
            @Parameter(description = "게시글 ID") Long postId,
            Long memberId,
            HttpServletRequest httpRequest
    );

    @Operation(
            summary = "게시글 목록 조회",
            description = "게시글 목록을 페이징하여 조회합니다."
    )
    @CustomErrorResponseDescription(SwaggerErrorResponseDescription.POST_LIST)
    ApiResponse<PageResponse<PostSummaryResponse>> getPostPage(
            @Parameter(description = "페이지 번호", example = "0") Integer page,
            @Parameter(description = "페이지 크기", example = "20") Integer size,
            @Parameter(description = "정렬 기준 (필드명,방향). 다중 정렬 가능", example = "createdAt,desc") List<String> sort,
            @Parameter(description = "태그 목록 (OR 조건 - 하나라도 포함하면 조회)", example = "Java,Spring") List<String> tags
    );

    @Operation(
            summary = "게시글 좋아요",
            description = "게시글에 좋아요를 추가합니다."
    )
    @CustomErrorResponseDescription(SwaggerErrorResponseDescription.POST_LIKE)
    void likePost(
            @Parameter(description = "게시글 ID") Long postId,
            Long memberId
    );

    @Operation(
            summary = "게시글 좋아요 취소",
            description = "게시글의 좋아요를 취소합니다."
    )
    @CustomErrorResponseDescription(SwaggerErrorResponseDescription.POST_UNLIKE)
    void unlikePost(
            @Parameter(description = "게시글 ID") Long postId,
            Long memberId
    );
}

