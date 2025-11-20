package com.kakaotechbootcamp.community.application.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kakaotechbootcamp.community.application.common.dto.response.PageResponse;
import com.kakaotechbootcamp.community.application.post.dto.request.PostCreateRequest;
import com.kakaotechbootcamp.community.application.post.dto.request.PostUpdateRequest;
import com.kakaotechbootcamp.community.application.post.dto.response.PostResponse;
import com.kakaotechbootcamp.community.application.post.dto.response.PostSummaryResponse;
import com.kakaotechbootcamp.community.common.exception.CustomException;
import com.kakaotechbootcamp.community.common.exception.code.CommonErrorCode;
import com.kakaotechbootcamp.community.common.exception.code.MemberErrorCode;
import com.kakaotechbootcamp.community.common.exception.code.PostErrorCode;
import com.kakaotechbootcamp.community.config.EnableSqlLogging;
import com.kakaotechbootcamp.community.config.IntegrationTest;
import com.kakaotechbootcamp.community.domain.post.entity.Attachment;
import com.kakaotechbootcamp.community.domain.post.entity.Post;
import com.kakaotechbootcamp.community.domain.post.repository.AttachmentRepository;
import com.kakaotechbootcamp.community.domain.post.repository.PostRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Sql(scripts = "/sql/post-service-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class PostServiceIntegrationTest {

    @Autowired
    private PostService postService;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private AttachmentRepository attachmentRepository;

    private static final Long TEST_MEMBER1_ID = 1L;
    private static final Long TEST_MEMBER2_ID = 2L;
    private static final Long TEST_POST1_ID = 1L;
    private static final Long TEST_POST2_ID = 2L;

    @Test
    @EnableSqlLogging
    @DisplayName("게시글 생성 성공 - 첨부파일 없이")
    void createPost_WithoutAttachment_Success() {
        // given
        PostCreateRequest request = new PostCreateRequest(
                TEST_MEMBER1_ID,
                "새로운 게시글",
                "게시글 내용입니다.",
                null
        );

        // when
        PostResponse response = postService.createPost(request, TEST_MEMBER1_ID);

        // then
        assertThat(response).isNotNull();
        assertThat(response.title()).isEqualTo("새로운 게시글");
        assertThat(response.content()).isEqualTo("게시글 내용입니다.");
        assertThat(response.viewCount()).isEqualTo(0L);
        assertThat(response.likeCount()).isEqualTo(0L);

        // DB 검증
        Post savedPost = postRepository.findById(response.postId()).orElseThrow();
        assertThat(savedPost.getTitle()).isEqualTo("새로운 게시글");
        assertThat(savedPost.getMember().getId()).isEqualTo(TEST_MEMBER1_ID);
    }

    @Test
    @EnableSqlLogging
    @DisplayName("게시글 생성 성공 - 첨부파일과 함께")
    void createPost_WithAttachment_Success() {
        // given
        String imageUrl = "https://example.com/image.jpg";
        PostCreateRequest request = new PostCreateRequest(
                TEST_MEMBER1_ID,
                "이미지가 있는 게시글",
                "이미지 포함 내용",
                imageUrl
        );

        // when
        PostResponse response = postService.createPost(request, TEST_MEMBER1_ID);

        // then
        assertThat(response).isNotNull();
        assertThat(response.imageUrl()).isEqualTo(imageUrl);

        // DB 검증 - 첨부파일이 저장되었는지 확인
        Attachment attachment = attachmentRepository.findByPostId(response.postId()).orElse(null);
        assertThat(attachment).isNotNull();
        assertThat(attachment.getAttachmentUrl()).isEqualTo(imageUrl);
    }

    @Test
    @EnableSqlLogging
    @DisplayName("게시글 생성 실패 - 존재하지 않는 회원")
    void createPost_MemberNotFound_ThrowsException() {
        // given
        Long nonExistentMemberId = 99999L;
        PostCreateRequest request = new PostCreateRequest(
                nonExistentMemberId,
                "제목",
                "내용",
                null
        );

        // when & then
        assertThatThrownBy(() -> postService.createPost(request, nonExistentMemberId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", MemberErrorCode.USER_NOT_FOUND);
    }

    @Test
    @EnableSqlLogging
    @DisplayName("게시글 수정 성공")
    void updatePost_Success() {
        // given
        PostUpdateRequest request = new PostUpdateRequest(
                TEST_MEMBER1_ID,
                "수정된 제목",
                "수정된 내용",
                null
        );

        // when
        PostResponse response = postService.updatePost(TEST_POST1_ID, request, TEST_MEMBER1_ID);

        // then
        assertThat(response).isNotNull();
        assertThat(response.title()).isEqualTo("수정된 제목");
        assertThat(response.content()).isEqualTo("수정된 내용");

        // DB 검증
        Post updatedPost = postRepository.findById(TEST_POST1_ID).orElseThrow();
        assertThat(updatedPost.getTitle()).isEqualTo("수정된 제목");
        assertThat(updatedPost.getContent()).isEqualTo("수정된 내용");
    }

    @Test
    @EnableSqlLogging
    @DisplayName("게시글 수정 실패 - 권한 없음 (다른 사용자)")
    void updatePost_NotOwner_ThrowsException() {
        // given
        PostUpdateRequest request = new PostUpdateRequest(
                TEST_MEMBER2_ID,
                "수정된 제목",
                "수정된 내용",
                null
        );

        // when & then - TEST_POST1_ID는 TEST_MEMBER1_ID 소유이므로 TEST_MEMBER2_ID로 수정 시도 시 실패
        assertThatThrownBy(() -> postService.updatePost(TEST_POST1_ID, request, TEST_MEMBER2_ID))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", CommonErrorCode.NO_PERMISSION);
    }

    @Test
    @EnableSqlLogging
    @DisplayName("게시글 수정 실패 - 존재하지 않는 게시글")
    void updatePost_PostNotFound_ThrowsException() {
        // given
        Long nonExistentPostId = 99999L;
        PostUpdateRequest request = new PostUpdateRequest(
                TEST_MEMBER1_ID,
                "수정된 제목",
                "수정된 내용",
                null
        );

        // when & then
        assertThatThrownBy(() -> postService.updatePost(nonExistentPostId, request, TEST_MEMBER1_ID))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", PostErrorCode.POST_NOT_FOUND);
    }

    @Test
    @EnableSqlLogging
    @DisplayName("게시글 삭제 성공")
    void deletePost_Success() {
        // when
        postService.deletePost(TEST_POST1_ID, TEST_MEMBER1_ID);

        // then - DB 검증 (소프트 삭제이므로 deleted 상태여야 한다.
        Post deletedPost = postRepository.findById(TEST_POST1_ID).orElse(null);

        assertThat(deletedPost).isNotNull();
        assertThat(deletedPost.isDeleted()).isTrue();
    }

    @Test
    @EnableSqlLogging
    @DisplayName("게시글 삭제 실패 - 권한 없음 (다른 사용자)")
    void deletePost_NotOwner_ThrowsException() {
        // when & then - TEST_POST1_ID는 TEST_MEMBER1_ID 소유이므로 TEST_MEMBER2_ID로 삭제 시도 시 실패
        assertThatThrownBy(() -> postService.deletePost(TEST_POST1_ID, TEST_MEMBER2_ID))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", CommonErrorCode.NO_PERMISSION);
    }

    @Test
    @EnableSqlLogging
    @DisplayName("게시글 상세 조회 성공")
    void getPostDetails_Success() {
        // when
        PostResponse response = postService.getPostDetails(TEST_POST1_ID, null);

        // then
        assertThat(response).isNotNull();
        assertThat(response.postId()).isEqualTo(TEST_POST1_ID);
        assertThat(response.title()).isEqualTo("Test Post 1");
        assertThat(response.content()).isEqualTo("Test Content 1");
        assertThat(response.member().nickname()).isEqualTo("tester1");
    }

    @Test
    @EnableSqlLogging
    @DisplayName("게시글 상세 조회 실패 - 존재하지 않는 게시글")
    void getPostDetails_PostNotFound_ThrowsException() {
        // given
        Long nonExistentPostId = 99999L;

        // when & then
        assertThatThrownBy(() -> postService.getPostDetails(nonExistentPostId, null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", PostErrorCode.POST_NOT_FOUND);
    }

    @Test
    @EnableSqlLogging
    @DisplayName("게시글 상세 조회 실패 - 삭제된 게시글")
    void getPostDetails_DeletedPost_ThrowsException() {
        // given - 게시글을 삭제
        postService.deletePost(TEST_POST1_ID, TEST_MEMBER1_ID);

        // when & then
        assertThatThrownBy(() -> postService.getPostDetails(TEST_POST1_ID, null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", PostErrorCode.POST_NOT_FOUND);
    }

    @Test
    @EnableSqlLogging
    @DisplayName("게시글 목록 조회 성공 - 페이징")
    void getPostPage_Success() {
        // given
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        // when
        PageResponse<PostSummaryResponse> page = postService.getPostPage(pageable);

        // then
        assertThat(page).isNotNull();
        assertThat(page.items()).hasSize(2); // test-data.sql에 2개의 게시글
        assertThat(page.totalElements()).isEqualTo(2L);

        PostSummaryResponse firstPost = page.items().get(0);
        assertThat(firstPost.postId()).isNotNull();
        assertThat(firstPost.title()).isNotNull();
        assertThat(firstPost.member().nickname()).isNotNull();
    }

    @Test
    @EnableSqlLogging
    @DisplayName("게시글 목록 조회 성공 - 삭제된 게시글은 제외")
    void getPostPage_ExcludesDeletedPosts() {
        // given
        postService.deletePost(TEST_POST1_ID, TEST_MEMBER1_ID);
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        // when
        PageResponse<PostSummaryResponse> page = postService.getPostPage(pageable);

        // then
        assertThat(page.totalElements()).isEqualTo(1L); // 삭제된 게시글은 제외되어야 함
        assertThat(page.items())
                .noneMatch(post -> post.postId().equals(TEST_POST1_ID));
    }

    @Test
    @EnableSqlLogging
    @DisplayName("게시글 수정 시 첨부파일 추가")
    @Transactional
    void updatePost_AddAttachment_Success() {
        // given
        String newImageUrl = "https://example.com/new-image.jpg";
        PostUpdateRequest request = new PostUpdateRequest(
                TEST_MEMBER1_ID,
                "수정된 제목",
                "수정된 내용",
                newImageUrl
        );

        // when
        PostResponse response = postService.updatePost(TEST_POST1_ID, request, TEST_MEMBER1_ID);

        // then
        assertThat(response.imageUrl()).isEqualTo(newImageUrl);

        // DB 검증
        Attachment attachment = attachmentRepository.findByPostId(TEST_POST1_ID).orElse(null);
        assertThat(attachment).isNotNull();
        assertThat(attachment.getAttachmentUrl()).isEqualTo(newImageUrl);
    }

    @Test
    @EnableSqlLogging
    @DisplayName("게시글 삭제 실패 - 이미 삭제된 게시글")
    void deletePost_AlreadyDeleted_ThrowsException() {
        // given - 먼저 게시글 삭제
        postService.deletePost(TEST_POST1_ID, TEST_MEMBER1_ID);

        // when & then - 다시 삭제 시도
        assertThatThrownBy(() -> postService.deletePost(TEST_POST1_ID, TEST_MEMBER1_ID))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", PostErrorCode.POST_NOT_FOUND);
    }

    @Test
    @EnableSqlLogging
    @DisplayName("게시글 생성 후 즉시 조회 가능")
    @Transactional
    void createPost_ThenRetrieve_Success() {
        // given
        PostCreateRequest createRequest = new PostCreateRequest(
                TEST_MEMBER1_ID,
                "즉시 조회 테스트",
                "생성 후 바로 조회",
                null
        );

        // when
        PostResponse createdPost = postService.createPost(createRequest, TEST_MEMBER1_ID);
        PostResponse retrievedPost = postService.getPostDetails(createdPost.postId(), null);

        // then
        assertThat(retrievedPost.postId()).isEqualTo(createdPost.postId());
        assertThat(retrievedPost.title()).isEqualTo("즉시 조회 테스트");
        assertThat(retrievedPost.content()).isEqualTo("생성 후 바로 조회");
    }

    @Test
    @EnableSqlLogging
    @DisplayName("여러 게시글 생성 후 목록 조회")
    @Transactional
    void createMultiplePosts_ThenRetrieveList_Success() {
        // given - 3개의 게시글 추가 생성
        for (int i = 0; i < 3; i++) {
            PostCreateRequest request = new PostCreateRequest(
                    TEST_MEMBER1_ID,
                    "추가 게시글 " + i,
                    "추가 내용 " + i,
                    null
            );
            postService.createPost(request, TEST_MEMBER1_ID);
        }

        // when
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        PageResponse<PostSummaryResponse> page = postService.getPostPage(pageable);

        // then
        assertThat(page.totalElements()).isEqualTo(5L); // 기존 2개 + 신규 3개
    }
}
