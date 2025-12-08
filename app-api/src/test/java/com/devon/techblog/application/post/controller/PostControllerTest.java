package com.devon.techblog.application.post.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.devon.techblog.application.common.dto.response.PageResponse;
import com.devon.techblog.application.member.dto.response.MemberResponse;
import com.devon.techblog.application.post.PostRequestFixture;
import com.devon.techblog.application.post.dto.request.PostCreateRequest;
import com.devon.techblog.application.post.dto.request.PostUpdateRequest;
import com.devon.techblog.application.post.dto.response.PostResponse;
import com.devon.techblog.application.post.dto.response.PostSummaryResponse;
import com.devon.techblog.application.post.service.PostLikeService;
import com.devon.techblog.application.post.service.PostService;
import com.devon.techblog.application.post.service.PostViewService;
import com.devon.techblog.common.exception.BusinessException;
import com.devon.techblog.common.exception.code.PostErrorCode;
import com.devon.techblog.config.annotation.ControllerWebMvcTest;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@ControllerWebMvcTest(PostController.class)
class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PostService postService;

    @MockitoBean
    private PostLikeService postLikeService;

    @MockitoBean
    private PostViewService postViewService;

    @Test
    @DisplayName("게시글 생성 - 201 Created")
    void createPost_success() throws Exception {
        PostCreateRequest request = PostRequestFixture.createRequest();
        MemberResponse memberResponse = new MemberResponse(1L, "tester", null);
        PostResponse response = new PostResponse(1L, memberResponse, "제목", "내용", null, Instant.now(), Instant.now(), 0L, 0L, 0L, false, null, null, null, null, "public");

        given(postService.createPost(any(), any())).willReturn(response);

        mockMvc.perform(post("/api/v1/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.postId").value(1L))
                .andExpect(jsonPath("$.data.title").value("제목"))
                .andExpect(jsonPath("$.data.content").value("내용"));
    }

    @Test
    @DisplayName("게시글 수정 - 200 OK")
    void updatePost_success() throws Exception {
        PostUpdateRequest request = PostRequestFixture.updateRequest();
        MemberResponse memberResponse = new MemberResponse(1L, "tester", null);
        PostResponse response = new PostResponse(1L, memberResponse, "수정된제목", "수정된내용", null, Instant.now(), Instant.now(), 0L, 0L, 0L, false, null, null, null, null, "public");

        given(postService.updatePost(any(), any(), any())).willReturn(response);

        mockMvc.perform(patch("/api/v1/posts/{postId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("수정된제목"))
                .andExpect(jsonPath("$.data.content").value("수정된내용"));
    }

    @Test
    @DisplayName("게시글 삭제 - 204 No Content")
    void deletePost_success() throws Exception {
        mockMvc.perform(delete("/api/v1/posts/{postId}", 1L))
                .andExpect(status().isNoContent());

        verify(postService).deletePost(any(), any());
    }

    @Test
    @DisplayName("게시글 단건 조회 - 200 OK")
    void getPost_success() throws Exception {
        MemberResponse memberResponse = new MemberResponse(1L, "tester", null);
        PostResponse response = new PostResponse(1L, memberResponse, "제목", "내용", null, Instant.now(), Instant.now(), 10L, 5L, 0L, false, null, null, null, null, "public");

        given(postService.getPostDetails(any(), any())).willReturn(response);

        mockMvc.perform(get("/api/v1/posts/{postId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.postId").value(1L))
                .andExpect(jsonPath("$.data.title").value("제목"))
                .andExpect(jsonPath("$.data.viewCount").value(10L));

        verify(postViewService).incrementViewCount(any(), any());
    }

    @Test
    @DisplayName("게시글 목록 조회 - 200 OK")
    void getPostPage_success() throws Exception {
        MemberResponse memberResponse = new MemberResponse(1L, "tester", null);
        PostSummaryResponse summary = new PostSummaryResponse(1L, "제목", memberResponse, Instant.now(), 10L, 5L, 3L, null, null);
        PageResponse<PostSummaryResponse> response = new PageResponse<>(List.of(summary), 0, 10, 1, 1);

        given(postService.getPostPage(any())).willReturn(response);

        mockMvc.perform(get("/api/v1/posts")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].postId").value(1L))
                .andExpect(jsonPath("$.data.items[0].title").value("제목"));
    }

    @Test
    @DisplayName("게시글 좋아요 - 204 No Content")
    void likePost_success() throws Exception {
        mockMvc.perform(post("/api/v1/posts/{postId}/like", 1L))
                .andExpect(status().isNoContent());

        verify(postLikeService).likePost(any(), any());
    }

    @Test
    @DisplayName("게시글 좋아요 취소 - 204 No Content")
    void unlikePost_success() throws Exception {
        mockMvc.perform(delete("/api/v1/posts/{postId}/like", 1L))
                .andExpect(status().isNoContent());

        verify(postLikeService).unlikePost(any(), any());
    }

    @Test
    @DisplayName("게시글 생성 시 title 누락 - 400 Bad Request")
    void createPost_withoutTitle_returns400() throws Exception {
        PostCreateRequest request = PostRequestFixture.createRequestWithoutTitle();

        mockMvc.perform(post("/api/v1/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("게시글 생성 시 content 누락 - 400 Bad Request")
    void createPost_withoutContent_returns400() throws Exception {
        PostCreateRequest request = PostRequestFixture.createRequestWithoutContent();

        mockMvc.perform(post("/api/v1/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("게시글 생성 시 잘못된 이미지 URL - 400 Bad Request")
    void createPost_withInvalidImageUrl_returns400() throws Exception {
        PostCreateRequest request = PostRequestFixture.createRequestWithInvalidImage("invalid-url");

        mockMvc.perform(post("/api/v1/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("게시글 수정 시 잘못된 썸네일 URL - 400 Bad Request")
    void updatePost_withInvalidThumbnailUrl_returns400() throws Exception {
        PostUpdateRequest request = PostRequestFixture.updateRequestWithInvalidThumbnail("not-a-url");

        mockMvc.perform(patch("/api/v1/posts/{postId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("존재하지 않는 게시글 조회 - 404 Not Found")
    void getPost_notFound_returns404() throws Exception {
        willThrow(new BusinessException(PostErrorCode.POST_NOT_FOUND))
                .given(postService).getPostDetails(any(), any());

        mockMvc.perform(get("/api/v1/posts/{postId}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(PostErrorCode.POST_NOT_FOUND.getMessage()));
    }

    @Test
    @DisplayName("존재하지 않는 게시글 좋아요 - 404 Not Found")
    void likePost_notFound_returns404() throws Exception {
        willThrow(new BusinessException(PostErrorCode.POST_NOT_FOUND))
                .given(postLikeService).likePost(any(), any());

        mockMvc.perform(post("/api/v1/posts/{postId}/like", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(PostErrorCode.POST_NOT_FOUND.getMessage()));
    }

    @Test
    @DisplayName("존재하지 않는 게시글 좋아요 취소 - 404 Not Found")
    void unlikePost_notFound_returns404() throws Exception {
        willThrow(new BusinessException(PostErrorCode.POST_NOT_FOUND))
                .given(postLikeService).unlikePost(any(), any());

        mockMvc.perform(delete("/api/v1/posts/{postId}/like", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(PostErrorCode.POST_NOT_FOUND.getMessage()));
    }
}
