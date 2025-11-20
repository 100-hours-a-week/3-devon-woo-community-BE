package com.kakaotechbootcamp.community.application.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kakaotechbootcamp.community.common.exception.CustomException;
import com.kakaotechbootcamp.community.common.exception.code.PostErrorCode;
import com.kakaotechbootcamp.community.config.EnableSqlLogging;
import com.kakaotechbootcamp.community.config.IntegrationTest;
import com.kakaotechbootcamp.community.domain.post.entity.Post;
import com.kakaotechbootcamp.community.domain.post.repository.PostLikeRepository;
import com.kakaotechbootcamp.community.domain.post.repository.PostRepository;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

@IntegrationTest
@Sql(scripts = "/sql/post-like-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class PostLikeServiceIntegrationTest {

    @Autowired
    private PostLikeService postLikeService;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostLikeRepository postLikeRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final Long TEST_MEMBER2_ID = 2L;
    private static final Long TEST_POST_ID = 1L;

    @Test
    @EnableSqlLogging
    @DisplayName("게시글 좋아요 추가 성공")
    void likePost_Success() {
        // when
        postLikeService.likePost(TEST_POST_ID, TEST_MEMBER2_ID);

        // then - DB 검증
        Post updatedPost = postRepository.findById(TEST_POST_ID).orElseThrow();
        assertThat(updatedPost.getLikeCount()).isEqualTo(1L);

        boolean likeExists = postLikeRepository.existsByPostIdAndMemberId(TEST_POST_ID, TEST_MEMBER2_ID);
        assertThat(likeExists).isTrue();
    }

    @Test
    @EnableSqlLogging
    @DisplayName("게시글 좋아요 취소 성공")
    void unlikePost_Success() {
        // given - SQL로 좋아요 추가
        jdbcTemplate.update(
            "INSERT INTO post_like (post_id, member_id, created_at) VALUES (?, ?, CURRENT_TIMESTAMP)",
            TEST_POST_ID, TEST_MEMBER2_ID
        );
        jdbcTemplate.update("UPDATE post SET like_count = 1 WHERE id = ?", TEST_POST_ID);

        // when
        postLikeService.unlikePost(TEST_POST_ID, TEST_MEMBER2_ID);

        // then - DB 검증
        Post updatedPost = postRepository.findById(TEST_POST_ID).orElseThrow();
        assertThat(updatedPost.getLikeCount()).isEqualTo(0L);

        boolean likeExists = postLikeRepository.existsByPostIdAndMemberId(TEST_POST_ID, TEST_MEMBER2_ID);
        assertThat(likeExists).isFalse();
    }

    @Test
    @EnableSqlLogging
    @DisplayName("존재하지 않는 게시글에 좋아요 시도 시 예외 발생")
    void likePost_PostNotFound_ThrowsException() {
        // given
        Long nonExistentPostId = 99999L;

        // when & then
        assertThatThrownBy(() -> postLikeService.likePost(nonExistentPostId, TEST_MEMBER2_ID))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", PostErrorCode.POST_NOT_FOUND);
    }

    @Test
    @EnableSqlLogging
    @DisplayName("중복 좋아요 시도 시 예외 발생")
    void likePost_AlreadyLiked_ThrowsException() {
        // given - 먼저 좋아요 추가
        postLikeService.likePost(TEST_POST_ID, TEST_MEMBER2_ID);

        // when & then - 다시 좋아요 시도
        assertThatThrownBy(() -> postLikeService.likePost(TEST_POST_ID, TEST_MEMBER2_ID))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", PostErrorCode.ALREADY_LIKED);
    }

    @Test
    @EnableSqlLogging
    @DisplayName("존재하지 않는 좋아요 취소 시도 시 예외 발생")
    void unlikePost_LikeNotFound_ThrowsException() {
        // when & then
        assertThatThrownBy(() -> postLikeService.unlikePost(TEST_POST_ID, TEST_MEMBER2_ID))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", PostErrorCode.LIKE_NOT_FOUND);
    }

    @Test
    @EnableSqlLogging
    @DisplayName("여러 사용자가 동시에 좋아요 추가 - 원자적 업데이트 검증")
    void likePost_ConcurrentLikes_AtomicUpdate() throws InterruptedException {
        // given
        int numberOfThreads = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        AtomicInteger successCount = new AtomicInteger(0);

        // 100명의 테스트 회원을 SQL로 생성
        for (int i = 0; i < numberOfThreads; i++) {
            long memberId = 100L + i;
            jdbcTemplate.update(
                "INSERT INTO member (id, email, password, nickname, profile_image_url, status) " +
                "VALUES (?, ?, ?, ?, ?, ?)",
                memberId,
                "concurrent" + i + "@example.com",
                "password123!",
                "concur" + i,
                "https://example.com/profile.jpg",
                "ACTIVE"
            );
        }

        // when - 100명이 동시에 좋아요 추가
        for (int i = 0; i < numberOfThreads; i++) {
            long memberId = 100L + i;
            executorService.execute(() -> {
                try {
                    postLikeService.likePost(TEST_POST_ID, memberId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // 예외는 무시 (중복 좋아요 등)
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then - 좋아요 수가 정확히 100개여야 함
        Post updatedPost = postRepository.findById(TEST_POST_ID).orElseThrow();
        assertThat(updatedPost.getLikeCount()).isEqualTo(numberOfThreads);
        assertThat(successCount.get()).isEqualTo(numberOfThreads);
    }

    @Test
    @EnableSqlLogging
    @DisplayName("동시에 좋아요 추가/취소 - 최종 카운트 일관성 검증")
    void likeAndUnlikePost_ConcurrentOperations_ConsistentCount() throws InterruptedException {
        // given
        int numberOfThreads = 100; // 50개는 좋아요, 50개는 좋아요 취소
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        // 100명의 테스트 회원 생성 및 좋아요 추가를 SQL로 처리
        for (int i = 0; i < numberOfThreads; i++) {
            long memberId = 200L + i;
            // 회원 생성
            jdbcTemplate.update(
                "INSERT INTO member (id, email, password, nickname, profile_image_url, status) " +
                "VALUES (?, ?, ?, ?, ?, ?)",
                memberId,
                "mixed" + i + "@example.com",
                "password123!",
                "mixed" + i,
                "https://example.com/profile.jpg",
                "ACTIVE"
            );

            // 좋아요 추가
            jdbcTemplate.update(
                "INSERT INTO post_like (post_id, member_id, created_at) VALUES (?, ?, CURRENT_TIMESTAMP)",
                TEST_POST_ID, memberId
            );
        }

        // 초기 좋아요 카운트 설정
        jdbcTemplate.update("UPDATE post SET like_count = ? WHERE id = ?", numberOfThreads, TEST_POST_ID);

        // when - 절반은 좋아요 취소, 절반은 재추가 시도 (실패할 것)
        for (int i = 0; i < numberOfThreads; i++) {
            long memberId = 200L + i;
            boolean shouldUnlike = i < numberOfThreads / 2;

            executorService.execute(() -> {
                try {
                    if (shouldUnlike) {
                        // 첫 50명은 좋아요 취소
                        postLikeService.unlikePost(TEST_POST_ID, memberId);
                    } else {
                        // 나머지 50명은 이미 좋아요 했으므로 재추가 시도 (실패)
                        try {
                            postLikeService.likePost(TEST_POST_ID, memberId);
                        } catch (CustomException e) {
                            // 중복 좋아요 예외 예상
                        }
                    }
                } catch (Exception e) {
                    // 무시
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then - 최종 좋아요 수는 50개여야 함 (100개 - 50개 취소)
        Post updatedPost = postRepository.findById(TEST_POST_ID).orElseThrow();
        assertThat(updatedPost.getLikeCount()).isEqualTo(numberOfThreads / 2);
    }
}
