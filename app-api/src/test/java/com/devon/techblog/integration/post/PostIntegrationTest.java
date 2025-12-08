package com.devon.techblog.integration.post;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.devon.techblog.application.post.PostRequestFixture;
import com.devon.techblog.application.post.dto.request.PostCreateRequest;
import com.devon.techblog.application.post.dto.request.PostUpdateRequest;
import com.devon.techblog.config.TestCurrentUserContext;
import com.devon.techblog.config.annotation.IntegrationTest;
import com.devon.techblog.domain.member.MemberFixture;
import com.devon.techblog.domain.member.entity.Member;
import com.devon.techblog.domain.member.repository.MemberRepository;
import com.devon.techblog.domain.post.PostFixture;
import com.devon.techblog.domain.post.entity.Post;
import com.devon.techblog.domain.post.repository.PostLikeRepository;
import com.devon.techblog.domain.post.repository.PostRepository;
import java.util.Optional;
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
class PostIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostLikeRepository postLikeRepository;

    @Autowired
    private TestCurrentUserContext currentUserContext;

    private Member savedMember;
    private Post savedPost;

    @BeforeEach
    void setUp() {
        savedMember = memberRepository.save(MemberFixture.create(
                "tester@example.com",
                "password123",
                "tester"
        ));
        savedPost = postRepository.save(PostFixture.create(savedMember));
        currentUserContext.setCurrentUserId(savedMember.getId());
    }

    @AfterEach
    void tearDown() {
        currentUserContext.clear();
    }

    @Test
    @DisplayName("통합 테스트 - 게시글 생성 시 201과 생성된 게시글을 반환한다")
    void createPost_returnsCreated_integration() throws Exception {
        PostCreateRequest request = PostRequestFixture.createRequest();

        mockMvc.perform(post("/api/v1/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value(PostFixture.DEFAULT_TITLE))
                .andExpect(jsonPath("$.data.content").value(PostFixture.DEFAULT_CONTENT));

        Assertions.assertThat(postRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("통합 테스트 - 게시글 수정 시 수정된 정보를 반환한다")
    void updatePost_returnsUpdated_integration() throws Exception {
        PostUpdateRequest request = PostRequestFixture.updateRequest();

        mockMvc.perform(patch("/api/v1/posts/{postId}", savedPost.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title", is(PostFixture.UPDATED_TITLE)))
                .andExpect(jsonPath("$.data.content", is(PostFixture.UPDATED_CONTENT)));

        Post updated = postRepository.findById(savedPost.getId()).orElseThrow();
        Assertions.assertThat(updated.getTitle()).isEqualTo(PostFixture.UPDATED_TITLE);
        Assertions.assertThat(updated.getContent()).isEqualTo(PostFixture.UPDATED_CONTENT);
    }

    @Test
    @DisplayName("통합 테스트 - 게시글 삭제 시 204를 반환하고 Post는 SoftDelete 된다")
    void deletePost_returnsNoContent_integration() throws Exception {
        mockMvc.perform(delete("/api/v1/posts/{postId}", savedPost.getId()))
                .andExpect(status().isNoContent());

        Optional<Post> post = postRepository.findById(savedPost.getId());
        Assertions.assertThat(post).isNotEmpty();
        Assertions.assertThat(post.get().getIsDeleted()).isTrue();
    }

    @Test
    @DisplayName("통합 테스트 - 게시글 단건 조회 시 상세 정보를 반환한다")
    void getPost_returnsPostDetails_integration() throws Exception {
        mockMvc.perform(get("/api/v1/posts/{postId}", savedPost.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value(PostFixture.DEFAULT_TITLE))
                .andExpect(jsonPath("$.data.content").value(PostFixture.DEFAULT_CONTENT))
                .andExpect(jsonPath("$.data.member.nickname").value("tester"));
    }

    @Test
    @DisplayName("통합 테스트 - 게시글 목록 조회 시 페이징된 목록을 반환한다")
    void getPostPage_returnsPagedPosts_integration() throws Exception {
        mockMvc.perform(get("/api/v1/posts")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.totalPages").value(1));
    }

    @Test
    @DisplayName("통합 테스트 - 게시글 좋아요 시 204를 반환한다")
    void likePost_returnsNoContent_integration() throws Exception {
        mockMvc.perform(post("/api/v1/posts/{postId}/like", savedPost.getId()))
                .andExpect(status().isNoContent());

        Assertions.assertThat(postLikeRepository.existsByPostIdAndMemberId(savedPost.getId(), savedMember.getId())).isTrue();
    }

    @Test
    @DisplayName("통합 테스트 - 게시글 좋아요 취소 시 204를 반환한다")
    void unlikePost_returnsNoContent_integration() throws Exception {
        mockMvc.perform(post("/api/v1/posts/{postId}/like", savedPost.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/v1/posts/{postId}/like", savedPost.getId()))
                .andExpect(status().isNoContent());

        Assertions.assertThat(postLikeRepository.existsByPostIdAndMemberId(savedPost.getId(), savedMember.getId())).isFalse();
    }

    @Test
    @DisplayName("통합 테스트 - 게시글 생성 후 조회 시 생성된 정보를 반환한다")
    void createPostThenGet_returnsCreatedPost() throws Exception {
        PostCreateRequest request = PostRequestFixture.createRequest("새 제목", "새 내용");

        String response = mockMvc.perform(post("/api/v1/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long postId = objectMapper.readTree(response).get("data").get("postId").asLong();

        mockMvc.perform(get("/api/v1/posts/{postId}", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("새 제목"))
                .andExpect(jsonPath("$.data.content").value("새 내용"));
    }

    @Test
    @DisplayName("통합 테스트 - 게시글 수정 후 조회 시 수정된 정보를 반환한다")
    void updatePostThenGet_returnsUpdatedPost() throws Exception {
        PostUpdateRequest request = PostRequestFixture.updateRequest();

        mockMvc.perform(patch("/api/v1/posts/{postId}", savedPost.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/posts/{postId}", savedPost.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value(PostFixture.UPDATED_TITLE))
                .andExpect(jsonPath("$.data.content").value(PostFixture.UPDATED_CONTENT));
    }

    @Test
    @DisplayName("통합 테스트 - 여러 게시글 생성 후 목록 조회 시 모두 반환한다")
    void createMultiplePosts_returnsAllInList() throws Exception {
        postRepository.save(PostFixture.create(savedMember, "제목2", "내용2"));
        postRepository.save(PostFixture.create(savedMember, "제목3", "내용3"));

        mockMvc.perform(get("/api/v1/posts")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.items.length()").value(3));

        Assertions.assertThat(postRepository.count()).isEqualTo(3);
    }

    @Test
    @DisplayName("통합 테스트 - 존재하지 않는 게시글 조회 시 404를 반환한다")
    void getPost_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/posts/{postId}", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("통합 테스트 - 존재하지 않는 게시글 수정 시 404를 반환한다")
    void updatePost_notFound_returns404() throws Exception {
        PostUpdateRequest request = PostRequestFixture.updateRequest();

        mockMvc.perform(patch("/api/v1/posts/{postId}", 999999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("통합 테스트 - 다른 회원의 게시글 수정 시 403을 반환한다")
    void updatePost_otherMember_returns403() throws Exception {
        Member otherMember = memberRepository.save(MemberFixture.create(
                "other@example.com",
                "password123",
                "other"
        ));
        currentUserContext.setCurrentUserId(otherMember.getId());

        PostUpdateRequest request = PostRequestFixture.updateRequest();

        mockMvc.perform(patch("/api/v1/posts/{postId}", savedPost.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("통합 테스트 - 다른 회원의 게시글 삭제 시 403을 반환한다")
    void deletePost_otherMember_returns403() throws Exception {
        Member otherMember = memberRepository.save(MemberFixture.create(
                "other@example.com",
                "password123",
                "other"
        ));
        currentUserContext.setCurrentUserId(otherMember.getId());

        mockMvc.perform(delete("/api/v1/posts/{postId}", savedPost.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("통합 테스트 - 잘못된 페이지 파라미터로 조회 시 적절히 처리한다")
    void getPostPage_invalidParams_handlesGracefully() throws Exception {
        mockMvc.perform(get("/api/v1/posts")
                        .param("page", "-1")
                        .param("size", "10"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("통합 테스트 - title 누락 시 400을 반환한다")
    void createPost_withoutTitle_returns400() throws Exception {
        PostCreateRequest request = PostRequestFixture.createRequestWithoutTitle();

        mockMvc.perform(post("/api/v1/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("통합 테스트 - content 누락 시 400을 반환한다")
    void createPost_withoutContent_returns400() throws Exception {
        PostCreateRequest request = PostRequestFixture.createRequestWithoutContent();

        mockMvc.perform(post("/api/v1/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
