package com.kakaotechbootcamp.community.integration.post;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kakaotechbootcamp.community.application.post.PostRequestFixture;
import com.kakaotechbootcamp.community.application.post.dto.request.PostCreateRequest;
import com.kakaotechbootcamp.community.application.post.dto.request.PostUpdateRequest;
import com.kakaotechbootcamp.community.config.TestSecurityConfig.TestCurrentUserContext;
import com.kakaotechbootcamp.community.config.annotation.IntegrationTest;
import com.kakaotechbootcamp.community.domain.member.MemberFixture;
import com.kakaotechbootcamp.community.domain.member.entity.Member;
import com.kakaotechbootcamp.community.domain.member.repository.MemberRepository;
import com.kakaotechbootcamp.community.domain.post.PostFixture;
import com.kakaotechbootcamp.community.domain.post.entity.Post;
import com.kakaotechbootcamp.community.domain.post.repository.PostLikeRepository;
import com.kakaotechbootcamp.community.domain.post.repository.PostRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
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
        postRepository.deleteAll();
        memberRepository.deleteAll();
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
        postRepository.deleteAll();
        memberRepository.deleteAll();
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
                .andExpect(jsonPath("$.message").value("post_created"))
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
                .andExpect(jsonPath("$.message").value("post_updated"))
                .andExpect(jsonPath("$.data.title", is(PostFixture.UPDATED_TITLE)))
                .andExpect(jsonPath("$.data.content", is(PostFixture.UPDATED_CONTENT)));

        Post updated = postRepository.findById(savedPost.getId()).orElseThrow();
        Assertions.assertThat(updated.getTitle()).isEqualTo(PostFixture.UPDATED_TITLE);
        Assertions.assertThat(updated.getContent()).isEqualTo(PostFixture.UPDATED_CONTENT);
    }

    @Test
    @Disabled("실제 비즈니스 로직 확인 필요")
    @DisplayName("통합 테스트 - 게시글 삭제 시 204를 반환한다")
    void deletePost_returnsNoContent_integration() throws Exception {
        mockMvc.perform(delete("/api/v1/posts/{postId}", savedPost.getId()))
                .andExpect(status().isNoContent());

        Assertions.assertThat(postRepository.findById(savedPost.getId())).isEmpty();
    }

    @Test
    @DisplayName("통합 테스트 - 게시글 단건 조회 시 상세 정보를 반환한다")
    void getPost_returnsPostDetails_integration() throws Exception {
        mockMvc.perform(get("/api/v1/posts/{postId}", savedPost.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("post_retrieved"))
                .andExpect(jsonPath("$.data.title").value(PostFixture.DEFAULT_TITLE))
                .andExpect(jsonPath("$.data.content").value(PostFixture.DEFAULT_CONTENT))
                .andExpect(jsonPath("$.data.member.nickname").value("tester"));
    }

    @Test
    @Disabled("실제 비즈니스 로직 확인 필요")
    @DisplayName("통합 테스트 - 게시글 목록 조회 시 페이징된 목록을 반환한다")
    void getPostPage_returnsPagedPosts_integration() throws Exception {
        mockMvc.perform(get("/api/v1/posts")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("posts_retrieved"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.totalPages").value(1));
    }

    @Test
    @Disabled("실제 비즈니스 로직 확인 필요")
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
    @Disabled("실제 비즈니스 로직 확인 필요")
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

        Long postId = objectMapper.readTree(response).get("data").get("id").asLong();

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
    @Disabled("실제 비즈니스 로직 확인 필요")
    @DisplayName("통합 테스트 - 여러 게시글 생성 후 목록 조회 시 모두 반환한다")
    void createMultiplePosts_returnsAllInList() throws Exception {
        postRepository.save(PostFixture.create(savedMember, "제목2", "내용2"));
        postRepository.save(PostFixture.create(savedMember, "제목3", "내용3"));

        mockMvc.perform(get("/api/v1/posts")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.content.length()").value(3));

        Assertions.assertThat(postRepository.count()).isEqualTo(3);
    }
}
