package com.kakaotechbootcamp.community.application.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kakaotechbootcamp.community.application.comment.dto.request.CommentCreateRequest;
import com.kakaotechbootcamp.community.application.comment.dto.request.CommentUpdateRequest;
import com.kakaotechbootcamp.community.application.comment.dto.response.CommentResponse;
import com.kakaotechbootcamp.community.application.common.dto.response.PageResponse;
import com.kakaotechbootcamp.community.common.exception.CustomException;
import com.kakaotechbootcamp.community.common.exception.code.CommentErrorCode;
import com.kakaotechbootcamp.community.common.exception.code.CommonErrorCode;
import com.kakaotechbootcamp.community.common.exception.code.MemberErrorCode;
import com.kakaotechbootcamp.community.common.exception.code.PostErrorCode;
import com.kakaotechbootcamp.community.config.EnableSqlLogging;
import com.kakaotechbootcamp.community.config.IntegrationTest;
import com.kakaotechbootcamp.community.domain.post.entity.Comment;
import com.kakaotechbootcamp.community.domain.post.entity.Post;
import com.kakaotechbootcamp.community.domain.post.repository.CommentRepository;
import com.kakaotechbootcamp.community.domain.post.repository.PostRepository;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Sql(scripts = "/sql/comment-service-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class CommentServiceIntegrationTest {

    @Autowired
    private CommentService commentService;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private PostRepository postRepository;

    private static final Long TEST_MEMBER1_ID = 1L;
    private static final Long TEST_MEMBER2_ID = 2L;
    private static final Long TEST_POST1_ID = 1L;
    private static final Long TEST_POST2_ID = 2L;
    private static final Long TEST_COMMENT1_ID = 1L;
    private static final Long TEST_COMMENT2_ID = 2L;

    @Test
    @EnableSqlLogging
    @DisplayName("댓글 생성 성공")
    void createComment_Success() {
        // given
        CommentCreateRequest request = new CommentCreateRequest(
                TEST_MEMBER1_ID,
                "새로운 댓글입니다."
        );

        // when
        CommentResponse response = commentService.createComment(TEST_POST1_ID, request, TEST_MEMBER1_ID);

        // then
        assertThat(response).isNotNull();
        assertThat(response.content()).isEqualTo("새로운 댓글입니다.");
        assertThat(response.postId()).isEqualTo(TEST_POST1_ID);
        assertThat(response.member().memberId()).isEqualTo(TEST_MEMBER1_ID);

        // DB 검증
        Comment savedComment = commentRepository.findById(response.commentId()).orElseThrow();
        assertThat(savedComment.getContent()).isEqualTo("새로운 댓글입니다.");
        assertThat(savedComment.getPost().getId()).isEqualTo(TEST_POST1_ID);
        assertThat(savedComment.getMember().getId()).isEqualTo(TEST_MEMBER1_ID);
    }

    @Test
    @EnableSqlLogging
    @DisplayName("댓글 생성 실패 - 존재하지 않는 게시글")
    void createComment_PostNotFound_ThrowsException() {
        // given
        Long nonExistentPostId = 99999L;
        CommentCreateRequest request = new CommentCreateRequest(
                TEST_MEMBER1_ID,
                "댓글 내용"
        );

        // when & then
        assertThatThrownBy(() -> commentService.createComment(nonExistentPostId, request, TEST_MEMBER1_ID))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", PostErrorCode.POST_NOT_FOUND);
    }

    @Test
    @EnableSqlLogging
    @DisplayName("댓글 생성 실패 - 존재하지 않는 회원")
    void createComment_MemberNotFound_ThrowsException() {
        // given
        Long nonExistentMemberId = 99999L;
        CommentCreateRequest request = new CommentCreateRequest(
                nonExistentMemberId,
                "댓글 내용"
        );

        // when & then
        assertThatThrownBy(() -> commentService.createComment(TEST_POST1_ID, request, nonExistentMemberId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", MemberErrorCode.USER_NOT_FOUND);
    }

    @Test
    @EnableSqlLogging
    @DisplayName("댓글 수정 성공")
    void updateComment_Success() {
        // given
        CommentUpdateRequest request = new CommentUpdateRequest(
                TEST_MEMBER1_ID,
                "수정된 댓글 내용"
        );

        // when
        CommentResponse response = commentService.updateComment(TEST_COMMENT1_ID, request, TEST_MEMBER1_ID);

        // then
        assertThat(response).isNotNull();
        assertThat(response.content()).isEqualTo("수정된 댓글 내용");
        assertThat(response.commentId()).isEqualTo(TEST_COMMENT1_ID);

        // DB 검증
        Comment updatedComment = commentRepository.findById(TEST_COMMENT1_ID).orElseThrow();
        assertThat(updatedComment.getContent()).isEqualTo("수정된 댓글 내용");
    }

    @Test
    @EnableSqlLogging
    @DisplayName("댓글 수정 실패 - 권한 없음 (다른 사용자)")
    void updateComment_NotOwner_ThrowsException() {
        // given
        CommentUpdateRequest request = new CommentUpdateRequest(
                TEST_MEMBER2_ID,
                "수정된 댓글 내용"
        );

        // when & then - TEST_COMMENT1_ID는 TEST_MEMBER1_ID 소유이므로 TEST_MEMBER2_ID로 수정 시도 시 실패
        assertThatThrownBy(() -> commentService.updateComment(TEST_COMMENT1_ID, request, TEST_MEMBER2_ID))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", CommonErrorCode.NO_PERMISSION);
    }

    @Test
    @EnableSqlLogging
    @DisplayName("댓글 수정 실패 - 존재하지 않는 댓글")
    void updateComment_CommentNotFound_ThrowsException() {
        // given
        Long nonExistentCommentId = 99999L;
        CommentUpdateRequest request = new CommentUpdateRequest(
                TEST_MEMBER1_ID,
                "수정된 댓글 내용"
        );

        // when & then
        assertThatThrownBy(() -> commentService.updateComment(nonExistentCommentId, request, TEST_MEMBER1_ID))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", CommentErrorCode.COMMENT_NOT_FOUND);
    }

    @Test
    @EnableSqlLogging
    @DisplayName("댓글 삭제 성공")
    void deleteComment_Success() {
        // when
        commentService.deleteComment(TEST_COMMENT1_ID, TEST_MEMBER1_ID);

        // then - DB 검증 (실제 삭제되므로 존재하지 않아야 함)
        boolean exists = commentRepository.existsById(TEST_COMMENT1_ID);
        assertThat(exists).isFalse();
    }

    @Test
    @EnableSqlLogging
    @DisplayName("댓글 삭제 실패 - 권한 없음 (다른 사용자)")
    void deleteComment_NotOwner_ThrowsException() {
        // when & then - TEST_COMMENT1_ID는 TEST_MEMBER1_ID 소유이므로 TEST_MEMBER2_ID로 삭제 시도 시 실패
        assertThatThrownBy(() -> commentService.deleteComment(TEST_COMMENT1_ID, TEST_MEMBER2_ID))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", CommonErrorCode.NO_PERMISSION);
    }

    @Test
    @EnableSqlLogging
    @DisplayName("댓글 삭제 실패 - 존재하지 않는 댓글")
    void deleteComment_CommentNotFound_ThrowsException() {
        // given
        Long nonExistentCommentId = 99999L;

        // when & then
        assertThatThrownBy(() -> commentService.deleteComment(nonExistentCommentId, TEST_MEMBER1_ID))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", CommentErrorCode.COMMENT_NOT_FOUND);
    }

    @Test
    @EnableSqlLogging
    @DisplayName("댓글 상세 조회 성공")
    void getCommentDetails_Success() {
        // when
        CommentResponse response = commentService.getCommentsDetails(TEST_COMMENT1_ID);

        // then
        assertThat(response).isNotNull();
        assertThat(response.commentId()).isEqualTo(TEST_COMMENT1_ID);
        assertThat(response.content()).isEqualTo("Test Comment 1");
        assertThat(response.postId()).isEqualTo(TEST_POST1_ID);
        assertThat(response.member().nickname()).isEqualTo("tester1");
    }

    @Test
    @EnableSqlLogging
    @DisplayName("댓글 상세 조회 실패 - 존재하지 않는 댓글")
    void getCommentDetails_CommentNotFound_ThrowsException() {
        // given
        Long nonExistentCommentId = 99999L;

        // when & then
        assertThatThrownBy(() -> commentService.getCommentsDetails(nonExistentCommentId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", CommentErrorCode.COMMENT_NOT_FOUND);
    }

    @Test
    @EnableSqlLogging
    @DisplayName("게시글의 댓글 목록 조회 성공 - 페이징")
    void getCommentPageByPostId_Success() {
        // given
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        // when
        PageResponse<CommentResponse> page = commentService.getCommentPageByPostId(TEST_POST1_ID, pageable);

        // then
        assertThat(page).isNotNull();
        assertThat(page.items()).hasSize(2); // test-data.sql에 게시글 1에 댓글 2개
        assertThat(page.totalElements()).isEqualTo(2L);

        CommentResponse firstComment = page.items().get(0);
        assertThat(firstComment.commentId()).isNotNull();
        assertThat(firstComment.content()).isNotNull();
        assertThat(firstComment.member().nickname()).isNotNull();
    }

    @Test
    @EnableSqlLogging
    @DisplayName("게시글의 댓글 목록 조회 실패 - 존재하지 않는 게시글")
    void getCommentPageByPostId_PostNotFound_ThrowsException() {
        // given
        Long nonExistentPostId = 99999L;
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        // when & then
        assertThatThrownBy(() -> commentService.getCommentPageByPostId(nonExistentPostId, pageable))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", PostErrorCode.POST_NOT_FOUND);
    }

    @Test
    @EnableSqlLogging
    @DisplayName("게시글의 댓글 목록 조회 성공 - 삭제된 댓글은 제외")
    void getCommentPageByPostId_ExcludesDeletedComments() {
        // given
        commentService.deleteComment(TEST_COMMENT1_ID, TEST_MEMBER1_ID);
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        // when
        PageResponse<CommentResponse> page = commentService.getCommentPageByPostId(TEST_POST1_ID, pageable);

        // then
        assertThat(page.totalElements()).isEqualTo(1L); // 삭제된 댓글은 제외되어야 함
        assertThat(page.items())
                .noneMatch(comment -> comment.commentId().equals(TEST_COMMENT1_ID));
    }

    @Test
    @EnableSqlLogging
    @DisplayName("댓글 생성 후 즉시 조회 가능")
    @Transactional
    void createComment_ThenRetrieve_Success() {
        // given
        CommentCreateRequest createRequest = new CommentCreateRequest(
                TEST_MEMBER1_ID,
                "즉시 조회 테스트 댓글"
        );

        // when
        CommentResponse createdComment = commentService.createComment(TEST_POST1_ID, createRequest, TEST_MEMBER1_ID);
        CommentResponse retrievedComment = commentService.getCommentsDetails(createdComment.commentId());

        // then
        assertThat(retrievedComment.commentId()).isEqualTo(createdComment.commentId());
        assertThat(retrievedComment.content()).isEqualTo("즉시 조회 테스트 댓글");
    }

    @Test
    @EnableSqlLogging
    @DisplayName("여러 댓글 생성 후 목록 조회")
    @Transactional
    void createMultipleComments_ThenRetrieveList_Success() {
        // given - 3개의 댓글 추가 생성
        for (int i = 0; i < 3; i++) {
            CommentCreateRequest request = new CommentCreateRequest(
                    TEST_MEMBER1_ID,
                    "추가 댓글 " + i
            );
            commentService.createComment(TEST_POST1_ID, request, TEST_MEMBER1_ID);
        }

        // when
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        PageResponse<CommentResponse> page = commentService.getCommentPageByPostId(TEST_POST1_ID, pageable);

        // then
        assertThat(page.totalElements()).isEqualTo(5L); // 기존 2개 + 신규 3개
    }

    @Test
    @EnableSqlLogging
    @DisplayName("댓글 삭제 후 재삭제 시도 시 예외 발생")
    void deleteComment_AlreadyDeleted_ThrowsException() {
        // given - 먼저 댓글 삭제
        commentService.deleteComment(TEST_COMMENT1_ID, TEST_MEMBER1_ID);

        // when & then - 다시 삭제 시도
        assertThatThrownBy(() -> commentService.deleteComment(TEST_COMMENT1_ID, TEST_MEMBER1_ID))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", CommentErrorCode.COMMENT_NOT_FOUND);
    }

    @Test
    @EnableSqlLogging
    @DisplayName("다른 게시글에는 댓글이 조회되지 않음")
    void getCommentPageByPostId_DifferentPost_ReturnsEmpty() {
        // given - TEST_POST2_ID에는 댓글이 없음
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        // when
        PageResponse<CommentResponse> page = commentService.getCommentPageByPostId(TEST_POST2_ID, pageable);

        // then
        assertThat(page.totalElements()).isEqualTo(0L);
        assertThat(page.items()).isEmpty();
    }

    @Test
    @EnableSqlLogging
    @DisplayName("댓글 추가 시 게시글의 commentCount가 증가함")
    @Transactional
    void createComment_IncrementsPostCommentCount() {
        // given - 초기 상태 확인
        Post post = postRepository.findById(TEST_POST1_ID).orElseThrow();
        Long initialCommentCount = post.getCommentCount();

        CommentCreateRequest request = new CommentCreateRequest(
                TEST_MEMBER1_ID,
                "새 댓글입니다"
        );

        // when
        commentService.createComment(TEST_POST1_ID, request, TEST_MEMBER1_ID);

        // then - commentCount가 1 증가해야 함
        Post updatedPost = postRepository.findById(TEST_POST1_ID).orElseThrow();
        assertThat(updatedPost.getCommentCount()).isEqualTo(initialCommentCount + 1);
    }

    @Test
    @EnableSqlLogging
    @DisplayName("댓글 삭제 시 게시글의 commentCount가 감소함")
    @Transactional
    void deleteComment_DecrementsPostCommentCount() {
        // given - 초기 상태 확인
        Post post = postRepository.findById(TEST_POST1_ID).orElseThrow();
        Long initialCommentCount = post.getCommentCount();

        // when
        commentService.deleteComment(TEST_COMMENT1_ID, TEST_MEMBER1_ID);

        // then - commentCount가 1 감소해야 함
        Post updatedPost = postRepository.findById(TEST_POST1_ID).orElseThrow();
        assertThat(updatedPost.getCommentCount()).isEqualTo(initialCommentCount - 1);
    }

    @Test
    @EnableSqlLogging
    @DisplayName("여러 댓글 추가 후 commentCount가 정확히 계산됨")
    @Transactional
    void createMultipleComments_CommentCountAccurate() {
        // given - 초기 상태 확인
        Post post = postRepository.findById(TEST_POST1_ID).orElseThrow();
        Long initialCommentCount = post.getCommentCount();

        // when - 3개의 댓글 추가
        for (int i = 0; i < 3; i++) {
            CommentCreateRequest request = new CommentCreateRequest(
                    TEST_MEMBER1_ID,
                    "댓글 " + i
            );
            commentService.createComment(TEST_POST1_ID, request, TEST_MEMBER1_ID);
        }

        // then - commentCount가 3 증가해야 함
        Post updatedPost = postRepository.findById(TEST_POST1_ID).orElseThrow();
        assertThat(updatedPost.getCommentCount()).isEqualTo(initialCommentCount + 3);
    }

    @Test
    @EnableSqlLogging
    @DisplayName("댓글 추가 후 삭제 시 commentCount가 원래대로 복구됨")
    @Transactional
    void createAndDeleteComment_CommentCountRestored() {
        // given - 초기 상태 확인
        Post initialPost = postRepository.findById(TEST_POST1_ID).orElseThrow();
        Long initialCommentCount = initialPost.getCommentCount();

        // when - 댓글 추가
        CommentCreateRequest request = new CommentCreateRequest(
                TEST_MEMBER1_ID,
                "임시 댓글"
        );
        CommentResponse createdComment = commentService.createComment(TEST_POST1_ID, request, TEST_MEMBER1_ID);

        // 추가 후 확인
        Post postAfterCreate = postRepository.findById(TEST_POST1_ID).orElseThrow();
        assertThat(postAfterCreate.getCommentCount()).isEqualTo(initialCommentCount + 1);

        // 댓글 삭제
        commentService.deleteComment(createdComment.commentId(), TEST_MEMBER1_ID);

        // then - commentCount가 원래대로 복구되어야 함
        Post finalPost = postRepository.findById(TEST_POST1_ID).orElseThrow();
        assertThat(finalPost.getCommentCount()).isEqualTo(initialCommentCount);
    }

    @Test
    @EnableSqlLogging
    @DisplayName("commentCount가 0일 때 댓글 삭제 시 음수가 되지 않음")
    @Transactional
    void deleteComment_CommentCountNotNegative() {
        // given - TEST_POST2_ID는 댓글이 없음 (commentCount = 0)
        Post post = postRepository.findById(TEST_POST2_ID).orElseThrow();
        assertThat(post.getCommentCount()).isEqualTo(0L);

        // 댓글 하나 추가
        CommentCreateRequest request = new CommentCreateRequest(
                TEST_MEMBER1_ID,
                "새 댓글"
        );
        CommentResponse createdComment = commentService.createComment(TEST_POST2_ID, request, TEST_MEMBER1_ID);

        // when - 댓글 삭제
        commentService.deleteComment(createdComment.commentId(), TEST_MEMBER1_ID);

        // then - commentCount가 0이어야 함 (음수 아님)
        Post updatedPost = postRepository.findById(TEST_POST2_ID).orElseThrow();
        assertThat(updatedPost.getCommentCount()).isEqualTo(0L);
    }

    @Test
    @EnableSqlLogging
    @DisplayName("서로 다른 게시글의 commentCount는 독립적으로 계산됨")
    @Transactional
    void commentCount_IndependentForDifferentPosts() {
        // given - 초기 상태 확인
        Post post1 = postRepository.findById(TEST_POST1_ID).orElseThrow();
        Post post2 = postRepository.findById(TEST_POST2_ID).orElseThrow();
        Long initialCount1 = post1.getCommentCount();
        Long initialCount2 = post2.getCommentCount();

        // when - POST1에만 댓글 추가
        CommentCreateRequest request = new CommentCreateRequest(
                TEST_MEMBER1_ID,
                "POST1의 댓글"
        );
        commentService.createComment(TEST_POST1_ID, request, TEST_MEMBER1_ID);

        // then - POST1의 commentCount만 증가하고 POST2는 그대로
        Post updatedPost1 = postRepository.findById(TEST_POST1_ID).orElseThrow();
        Post updatedPost2 = postRepository.findById(TEST_POST2_ID).orElseThrow();

        assertThat(updatedPost1.getCommentCount()).isEqualTo(initialCount1 + 1);
        assertThat(updatedPost2.getCommentCount()).isEqualTo(initialCount2);
    }

    @Test
    @EnableSqlLogging
    @DisplayName("100개의 댓글이 동시에 추가될 때 commentCount가 정확히 계산됨")
    void createComments_Concurrency_CommentCountAccurate() throws InterruptedException {
        // given
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        Post initialPost = postRepository.findById(TEST_POST1_ID).orElseThrow();
        Long initialCommentCount = initialPost.getCommentCount();

        // when - 100개의 스레드에서 동시에 댓글 추가
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    CommentCreateRequest request = new CommentCreateRequest(
                            TEST_MEMBER1_ID,
                            "동시성 테스트 댓글 " + index
                    );
                    commentService.createComment(TEST_POST1_ID, request, TEST_MEMBER1_ID);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    System.err.println("댓글 생성 실패 [" + index + "]: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        // 모든 스레드가 완료될 때까지 대기 (최대 30초)
        boolean completed = latch.await(30, TimeUnit.SECONDS);
        executorService.shutdown();

        // then
        assertThat(completed).isTrue(); // 모든 스레드가 완료되었는지 확인

        // DB에서 최종 상태 확인
        Post finalPost = postRepository.findById(TEST_POST1_ID).orElseThrow();
        long actualCommentCount = commentRepository.count();

        System.out.println("초기 commentCount: " + initialCommentCount);
        System.out.println("성공한 댓글 추가: " + successCount.get());
        System.out.println("실패한 댓글 추가: " + failCount.get());
        System.out.println("최종 commentCount (Post): " + finalPost.getCommentCount());
        System.out.println("실제 댓글 수 (DB): " + actualCommentCount);

        // commentCount가 성공적으로 추가된 댓글 수만큼 증가했는지 확인
        assertThat(finalPost.getCommentCount()).isEqualTo(initialCommentCount + successCount.get());

        // 실제 DB의 댓글 수와 Post의 commentCount가 일치하는지 확인
        long commentsForPost1 = commentRepository.findAll().stream()
                .filter(c -> c.getPost().getId().equals(TEST_POST1_ID))
                .count();
        assertThat(finalPost.getCommentCount()).isEqualTo(commentsForPost1);
    }

    @Test
    @EnableSqlLogging
    @DisplayName("동시에 댓글 추가와 삭제가 발생해도 commentCount가 정확히 계산됨")
    void createAndDeleteComments_Concurrency_CommentCountAccurate() throws InterruptedException {
        // given - 먼저 10개의 댓글을 추가
        Long[] commentIds = new Long[10];
        for (int i = 0; i < 10; i++) {
            CommentCreateRequest request = new CommentCreateRequest(
                    TEST_MEMBER1_ID,
                    "삭제용 댓글 " + i
            );
            CommentResponse response = commentService.createComment(TEST_POST1_ID, request, TEST_MEMBER1_ID);
            commentIds[i] = response.commentId();
        }

        Post postAfterSetup = postRepository.findById(TEST_POST1_ID).orElseThrow();
        Long initialCommentCount = postAfterSetup.getCommentCount();

        // when - 50개는 추가, 10개는 삭제를 동시에 실행
        int totalThreads = 60;
        ExecutorService executorService = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch latch = new CountDownLatch(totalThreads);
        AtomicInteger addSuccess = new AtomicInteger(0);
        AtomicInteger deleteSuccess = new AtomicInteger(0);

        // 50개 추가
        for (int i = 0; i < 50; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    CommentCreateRequest request = new CommentCreateRequest(
                            TEST_MEMBER1_ID,
                            "동시성 추가 댓글 " + index
                    );
                    commentService.createComment(TEST_POST1_ID, request, TEST_MEMBER1_ID);
                    addSuccess.incrementAndGet();
                } catch (Exception e) {
                    System.err.println("댓글 추가 실패: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        // 10개 삭제
        for (int i = 0; i < 10; i++) {
            final Long commentId = commentIds[i];
            executorService.submit(() -> {
                try {
                    commentService.deleteComment(commentId, TEST_MEMBER1_ID);
                    deleteSuccess.incrementAndGet();
                } catch (Exception e) {
                    System.err.println("댓글 삭제 실패: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        // 모든 스레드가 완료될 때까지 대기
        boolean completed = latch.await(30, TimeUnit.SECONDS);
        executorService.shutdown();

        // then
        assertThat(completed).isTrue();

        Post finalPost = postRepository.findById(TEST_POST1_ID).orElseThrow();

        System.out.println("초기 commentCount: " + initialCommentCount);
        System.out.println("추가 성공: " + addSuccess.get());
        System.out.println("삭제 성공: " + deleteSuccess.get());
        System.out.println("예상 commentCount: " + (initialCommentCount + addSuccess.get() - deleteSuccess.get()));
        System.out.println("실제 commentCount: " + finalPost.getCommentCount());

        // commentCount = 초기값 + 추가 성공 - 삭제 성공
        Long expectedCount = initialCommentCount + addSuccess.get() - deleteSuccess.get();
        assertThat(finalPost.getCommentCount()).isEqualTo(expectedCount);
    }
}
