package com.kakaotechbootcamp.community.application.post.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.kakaotechbootcamp.community.application.post.dto.ViewContext;
import com.kakaotechbootcamp.community.config.EnableSqlLogging;
import com.kakaotechbootcamp.community.config.IntegrationTest;
import com.kakaotechbootcamp.community.domain.post.entity.Post;
import com.kakaotechbootcamp.community.domain.post.repository.PostRepository;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

@IntegrationTest
@Sql(scripts = "/sql/post-service-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class PostViewServiceIntegrationTest {

    @Autowired
    private PostViewService postViewService;

    @Autowired
    private PostRepository postRepository;

    private static final Long TEST_POST1_ID = 1L;
    private static final Long TEST_MEMBER1_ID = 1L;

    @Test
    @EnableSqlLogging
    @DisplayName("조회수 증가 성공")
    void incrementViewCount_Success() {
        // given
        ViewContext context = ViewContext.builder()
                .memberId(TEST_MEMBER1_ID)
                .ipAddress("127.0.0.1")
                .userAgent("Mozilla/5.0")
                .build();

        Post initialPost = postRepository.findById(TEST_POST1_ID).orElseThrow();
        Long initialViewCount = initialPost.getViewsCount();

        // when
        postViewService.incrementViewCount(TEST_POST1_ID, context);

        // then
        Post updatedPost = postRepository.findById(TEST_POST1_ID).orElseThrow();
        assertThat(updatedPost.getViewsCount()).isEqualTo(initialViewCount + 1);
    }

    @Test
    @EnableSqlLogging
    @DisplayName("조회수 여러 번 증가 성공")
    void incrementViewCount_MultipleTimes_Success() {
        // given
        ViewContext context = ViewContext.builder()
                .memberId(TEST_MEMBER1_ID)
                .ipAddress("127.0.0.1")
                .userAgent("Mozilla/5.0")
                .build();

        Post initialPost = postRepository.findById(TEST_POST1_ID).orElseThrow();
        Long initialViewCount = initialPost.getViewsCount();

        // when - 5번 조회
        for (int i = 0; i < 5; i++) {
            postViewService.incrementViewCount(TEST_POST1_ID, context);
        }

        // then
        Post updatedPost = postRepository.findById(TEST_POST1_ID).orElseThrow();
        assertThat(updatedPost.getViewsCount()).isEqualTo(initialViewCount + 5);
    }

    @Test
    @EnableSqlLogging
    @DisplayName("로그인하지 않은 사용자도 조회수 증가 가능")
    void incrementViewCount_AnonymousUser_Success() {
        // given - memberId가 null인 익명 사용자
        ViewContext context = ViewContext.builder()
                .memberId(null)
                .ipAddress("192.168.1.1")
                .userAgent("Mozilla/5.0")
                .build();

        Post initialPost = postRepository.findById(TEST_POST1_ID).orElseThrow();
        Long initialViewCount = initialPost.getViewsCount();

        // when
        postViewService.incrementViewCount(TEST_POST1_ID, context);

        // then
        Post updatedPost = postRepository.findById(TEST_POST1_ID).orElseThrow();
        assertThat(updatedPost.getViewsCount()).isEqualTo(initialViewCount + 1);
    }

    @Test
    @EnableSqlLogging
    @DisplayName("다른 IP에서 조회 시 각각 조회수 증가")
    void incrementViewCount_DifferentIpAddresses_Success() {
        // given
        ViewContext context1 = ViewContext.builder()
                .memberId(TEST_MEMBER1_ID)
                .ipAddress("127.0.0.1")
                .userAgent("Mozilla/5.0")
                .build();

        ViewContext context2 = ViewContext.builder()
                .memberId(null)
                .ipAddress("192.168.1.1")
                .userAgent("Chrome/91.0")
                .build();

        Post initialPost = postRepository.findById(TEST_POST1_ID).orElseThrow();
        Long initialViewCount = initialPost.getViewsCount();

        // when
        postViewService.incrementViewCount(TEST_POST1_ID, context1);
        postViewService.incrementViewCount(TEST_POST1_ID, context2);

        // then
        Post updatedPost = postRepository.findById(TEST_POST1_ID).orElseThrow();
        assertThat(updatedPost.getViewsCount()).isEqualTo(initialViewCount + 2);
    }

    @Test
    @EnableSqlLogging
    @DisplayName("동시에 여러 사용자가 조회 시 조회수 원자적 증가")
    void incrementViewCount_ConcurrentViews_AtomicUpdate() throws InterruptedException {
        // given
        int numberOfThreads = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        AtomicInteger successCount = new AtomicInteger(0);

        Post initialPost = postRepository.findById(TEST_POST1_ID).orElseThrow();
        Long initialViewCount = initialPost.getViewsCount();

        // when - 100명이 동시에 조회
        for (int i = 0; i < numberOfThreads; i++) {
            int userId = i;
            executorService.execute(() -> {
                try {
                    ViewContext context = ViewContext.builder()
                            .memberId((long) userId)
                            .ipAddress("192.168.1." + userId)
                            .userAgent("Mozilla/5.0")
                            .build();
                    postViewService.incrementViewCount(TEST_POST1_ID, context);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // 예외 무시
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then - 조회수가 정확히 100 증가해야 함
        Post updatedPost = postRepository.findById(TEST_POST1_ID).orElseThrow();
        assertThat(updatedPost.getViewsCount()).isEqualTo(initialViewCount + numberOfThreads);
        assertThat(successCount.get()).isEqualTo(numberOfThreads);
    }

    @Test
    @EnableSqlLogging
    @DisplayName("존재하지 않는 게시글에 조회수 증가 시도 - 예외 발생하지 않음")
    void incrementViewCount_NonExistentPost_NoException() {
        // given
        Long nonExistentPostId = 99999L;
        ViewContext context = ViewContext.builder()
                .memberId(TEST_MEMBER1_ID)
                .ipAddress("127.0.0.1")
                .userAgent("Mozilla/5.0")
                .build();

        // when & then - 예외가 발생하지 않아야 함 (조용히 실패)
        postViewService.incrementViewCount(nonExistentPostId, context);
    }

    @Test
    @EnableSqlLogging
    @DisplayName("조회수 초기값 확인")
    void incrementViewCount_InitialViewCount_IsZero() {
        // given
        Post post = postRepository.findById(TEST_POST1_ID).orElseThrow();

        // then
        assertThat(post.getViewsCount()).isEqualTo(0L);
    }

    @Test
    @EnableSqlLogging
    @DisplayName("여러 게시글의 조회수 독립적으로 증가")
    void incrementViewCount_MultiplePostsIndependently_Success() {
        // given
        Long post1Id = 1L;
        Long post2Id = 2L;

        ViewContext context = ViewContext.builder()
                .memberId(TEST_MEMBER1_ID)
                .ipAddress("127.0.0.1")
                .userAgent("Mozilla/5.0")
                .build();

        Post initialPost1 = postRepository.findById(post1Id).orElseThrow();
        Post initialPost2 = postRepository.findById(post2Id).orElseThrow();

        Long initialViewCount1 = initialPost1.getViewsCount();
        Long initialViewCount2 = initialPost2.getViewsCount();

        // when
        postViewService.incrementViewCount(post1Id, context);
        postViewService.incrementViewCount(post1Id, context);
        postViewService.incrementViewCount(post2Id, context);

        // then
        Post updatedPost1 = postRepository.findById(post1Id).orElseThrow();
        Post updatedPost2 = postRepository.findById(post2Id).orElseThrow();

        assertThat(updatedPost1.getViewsCount()).isEqualTo(initialViewCount1 + 2);
        assertThat(updatedPost2.getViewsCount()).isEqualTo(initialViewCount2 + 1);
    }

    @Test
    @EnableSqlLogging
    @DisplayName("대량 조회수 증가 테스트")
    void incrementViewCount_LargeNumberOfViews_Success() {
        // given
        ViewContext context = ViewContext.builder()
                .memberId(TEST_MEMBER1_ID)
                .ipAddress("127.0.0.1")
                .userAgent("Mozilla/5.0")
                .build();

        Post initialPost = postRepository.findById(TEST_POST1_ID).orElseThrow();
        Long initialViewCount = initialPost.getViewsCount();
        int numberOfViews = 1000;

        // when
        for (int i = 0; i < numberOfViews; i++) {
            postViewService.incrementViewCount(TEST_POST1_ID, context);
        }

        // then
        Post updatedPost = postRepository.findById(TEST_POST1_ID).orElseThrow();
        assertThat(updatedPost.getViewsCount()).isEqualTo(initialViewCount + numberOfViews);
    }
}
