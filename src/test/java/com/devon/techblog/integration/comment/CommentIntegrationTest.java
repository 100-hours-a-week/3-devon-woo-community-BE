package com.devon.techblog.integration.comment;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.devon.techblog.application.comment.CommentRequestFixture;
import com.devon.techblog.application.comment.dto.request.CommentCreateRequest;
import com.devon.techblog.application.comment.dto.request.CommentUpdateRequest;
import com.devon.techblog.config.TestCurrentUserContext;
import com.devon.techblog.config.annotation.IntegrationTest;
import com.devon.techblog.domain.member.MemberFixture;
import com.devon.techblog.domain.member.entity.Member;
import com.devon.techblog.domain.member.repository.MemberRepository;
import com.devon.techblog.domain.post.CommentFixture;
import com.devon.techblog.domain.post.PostFixture;
import com.devon.techblog.domain.post.entity.Comment;
import com.devon.techblog.domain.post.entity.Post;
import com.devon.techblog.domain.post.repository.CommentRepository;
import com.devon.techblog.domain.post.repository.PostRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class CommentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private TestCurrentUserContext currentUserContext;

    private Member savedMember;
    private Post savedPost;
    private Comment savedComment;

    @BeforeEach
    void setUp() {
        savedMember = memberRepository.save(MemberFixture.create(
                "tester@example.com",
                "password123",
                "tester"
        ));
        savedPost = postRepository.save(PostFixture.create(savedMember));
        savedComment = commentRepository.save(CommentFixture.create(savedMember, savedPost));
        currentUserContext.setCurrentUserId(savedMember.getId());
    }

    @AfterEach
    void tearDown() {
        currentUserContext.clear();
    }

    @Test
    @DisplayName("통합 테스트 - 댓글 생성 시 201과 생성된 댓글을 반환한다")
    void createComment_returnsCreated_integration() throws Exception {
        CommentCreateRequest request = CommentRequestFixture.createRequest();

        mockMvc.perform(post("/api/v1/posts/{postId}/comments", savedPost.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("comment_created"))
                .andExpect(jsonPath("$.data.content").value(CommentFixture.DEFAULT_CONTENT));

        Assertions.assertThat(commentRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("통합 테스트 - 게시글의 댓글 목록 조회 시 페이징된 목록을 반환한다")
    void getCommentPage_returnsPagedComments_integration() throws Exception {
        mockMvc.perform(get("/api/v1/posts/{postId}/comments", savedPost.getId())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("comments_retrieved"))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.totalPages").value(1));
    }

    @Test
    @DisplayName("통합 테스트 - 댓글 단건 조회 시 상세 정보를 반환한다")
    void getComment_returnsCommentDetails_integration() throws Exception {
        mockMvc.perform(get("/api/v1/comments/{commentId}", savedComment.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("comment_fetched"))
                .andExpect(jsonPath("$.data.content").value(CommentFixture.DEFAULT_CONTENT))
                .andExpect(jsonPath("$.data.member.nickname").value("tester"));
    }

    @Test
    @DisplayName("통합 테스트 - 댓글 수정 시 수정된 정보를 반환한다")
    void updateComment_returnsUpdated_integration() throws Exception {
        CommentUpdateRequest request = CommentRequestFixture.updateRequest();

        mockMvc.perform(patch("/api/v1/comments/{commentId}", savedComment.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("comment_updated"))
                .andExpect(jsonPath("$.data.content", is(CommentFixture.UPDATED_CONTENT)));

        Comment updated = commentRepository.findById(savedComment.getId()).orElseThrow();
        Assertions.assertThat(updated.getContent()).isEqualTo(CommentFixture.UPDATED_CONTENT);
    }

    @Test
    @DisplayName("통합 테스트 - 댓글 삭제 시 204를 반환한다")
    void deleteComment_returnsNoContent_integration() throws Exception {
        mockMvc.perform(delete("/api/v1/comments/{commentId}", savedComment.getId()))
                .andExpect(status().isNoContent());

        Assertions.assertThat(commentRepository.findById(savedComment.getId())).isEmpty();
    }

    @Test
    @DisplayName("통합 테스트 - 댓글 생성 후 조회 시 생성된 정보를 반환한다")
    void createCommentThenGet_returnsCreatedComment() throws Exception {
        CommentCreateRequest request = CommentRequestFixture.createRequest("새 댓글");

        String response = mockMvc.perform(post("/api/v1/posts/{postId}/comments", savedPost.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long commentId = objectMapper.readTree(response).get("data").get("commentId").asLong();

        mockMvc.perform(get("/api/v1/comments/{commentId}", commentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("새 댓글"));
    }

    @Test
    @DisplayName("통합 테스트 - 댓글 수정 후 조회 시 수정된 정보를 반환한다")
    void updateCommentThenGet_returnsUpdatedComment() throws Exception {
        CommentUpdateRequest request = CommentRequestFixture.updateRequest();

        mockMvc.perform(patch("/api/v1/comments/{commentId}", savedComment.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/comments/{commentId}", savedComment.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value(CommentFixture.UPDATED_CONTENT));
    }

    @Test
    @DisplayName("통합 테스트 - 여러 댓글 생성 후 목록 조회 시 모두 반환한다")
    void createMultipleComments_returnsAllInList() throws Exception {
        commentRepository.save(CommentFixture.create(savedMember, savedPost, "댓글2"));
        commentRepository.save(CommentFixture.create(savedMember, savedPost, "댓글3"));

        mockMvc.perform(get("/api/v1/posts/{postId}/comments", savedPost.getId())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.items.length()").value(3));

        Assertions.assertThat(commentRepository.count()).isEqualTo(3);
    }
}
