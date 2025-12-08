package com.devon.techblog.application.comment.controller;

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
import com.devon.techblog.application.comment.CommentRequestFixture;
import com.devon.techblog.application.comment.dto.request.CommentCreateRequest;
import com.devon.techblog.application.comment.dto.request.CommentUpdateRequest;
import com.devon.techblog.application.comment.dto.response.CommentResponse;
import com.devon.techblog.application.comment.service.CommentService;
import com.devon.techblog.application.common.dto.response.PageResponse;
import com.devon.techblog.application.member.dto.response.MemberResponse;
import com.devon.techblog.common.exception.BusinessException;
import com.devon.techblog.common.exception.code.CommentErrorCode;
import com.devon.techblog.config.annotation.ControllerWebMvcTest;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@ControllerWebMvcTest(CommentController.class)
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CommentService commentService;

    @Test
    @DisplayName("댓글 생성 - 201 Created")
    void createComment_success() throws Exception {
        CommentCreateRequest request = CommentRequestFixture.createRequest();
        MemberResponse memberResponse = new MemberResponse(1L, "tester", null);
        CommentResponse response = new CommentResponse(1L, 1L, "댓글내용", memberResponse, Instant.now(), Instant.now());

        given(commentService.createComment(any(), any(), any())).willReturn(response);

        mockMvc.perform(post("/api/v1/posts/{postId}/comments", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.commentId").value(1L))
                .andExpect(jsonPath("$.data.content").value("댓글내용"));
    }

    @Test
    @DisplayName("게시글의 댓글 목록 조회 - 200 OK")
    void getCommentPage_success() throws Exception {
        MemberResponse memberResponse = new MemberResponse(1L, "tester", null);
        CommentResponse comment = new CommentResponse(1L, 1L, "댓글내용", memberResponse, Instant.now(), Instant.now());
        PageResponse<CommentResponse> response = new PageResponse<>(List.of(comment), 0, 10, 1, 1);

        given(commentService.getCommentPageByPostId(any(), any())).willReturn(response);

        mockMvc.perform(get("/api/v1/posts/{postId}/comments", 1L)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].commentId").value(1L))
                .andExpect(jsonPath("$.data.items[0].content").value("댓글내용"));
    }

    @Test
    @DisplayName("댓글 단건 조회 - 200 OK")
    void getComment_success() throws Exception {
        MemberResponse memberResponse = new MemberResponse(1L, "tester", null);
        CommentResponse response = new CommentResponse(1L, 1L, "댓글내용", memberResponse, Instant.now(), Instant.now());

        given(commentService.getCommentsDetails(any())).willReturn(response);

        mockMvc.perform(get("/api/v1/comments/{commentId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.commentId").value(1L))
                .andExpect(jsonPath("$.data.content").value("댓글내용"));
    }

    @Test
    @DisplayName("댓글 수정 - 200 OK")
    void updateComment_success() throws Exception {
        CommentUpdateRequest request = CommentRequestFixture.updateRequest();
        MemberResponse memberResponse = new MemberResponse(1L, "tester", null);
        CommentResponse response = new CommentResponse(1L, 1L, "수정된댓글", memberResponse, Instant.now(), Instant.now());

        given(commentService.updateComment(any(), any(), any())).willReturn(response);

        mockMvc.perform(patch("/api/v1/comments/{commentId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("수정된댓글"));
    }

    @Test
    @DisplayName("댓글 삭제 - 204 No Content")
    void deleteComment_success() throws Exception {
        mockMvc.perform(delete("/api/v1/comments/{commentId}", 1L))
                .andExpect(status().isNoContent());

        verify(commentService).deleteComment(any(), any());
    }

    @Test
    @DisplayName("댓글 생성 시 content 누락 - 400 Bad Request")
    void createComment_withoutContent_returns400() throws Exception {
        CommentCreateRequest request = CommentRequestFixture.createRequestWithoutContent();

        mockMvc.perform(post("/api/v1/posts/{postId}/comments", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("댓글 수정 시 content 누락 - 400 Bad Request")
    void updateComment_withoutContent_returns400() throws Exception {
        CommentUpdateRequest request = CommentRequestFixture.updateRequestWithoutContent();

        mockMvc.perform(patch("/api/v1/comments/{commentId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("존재하지 않는 댓글 조회 - 404 Not Found")
    void getComment_notFound_returns404() throws Exception {
        willThrow(new BusinessException(CommentErrorCode.COMMENT_NOT_FOUND))
                .given(commentService).getCommentsDetails(any());

        mockMvc.perform(get("/api/v1/comments/{commentId}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(CommentErrorCode.COMMENT_NOT_FOUND.getMessage()));
    }

    @Test
    @DisplayName("존재하지 않는 댓글 수정 - 404 Not Found")
    void updateComment_notFound_returns404() throws Exception {
        CommentUpdateRequest request = CommentRequestFixture.updateRequest();

        willThrow(new BusinessException(CommentErrorCode.COMMENT_NOT_FOUND))
                .given(commentService).updateComment(any(), any(), any());

        mockMvc.perform(patch("/api/v1/comments/{commentId}", 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(CommentErrorCode.COMMENT_NOT_FOUND.getMessage()));
    }

    @Test
    @DisplayName("존재하지 않는 댓글 삭제 - 404 Not Found")
    void deleteComment_notFound_returns404() throws Exception {
        willThrow(new BusinessException(CommentErrorCode.COMMENT_NOT_FOUND))
                .given(commentService).deleteComment(any(), any());

        mockMvc.perform(delete("/api/v1/comments/{commentId}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(CommentErrorCode.COMMENT_NOT_FOUND.getMessage()));
    }
}
