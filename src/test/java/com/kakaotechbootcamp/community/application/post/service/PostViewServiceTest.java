package com.kakaotechbootcamp.community.application.post.service;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.kakaotechbootcamp.community.application.post.dto.ViewContext;
import com.kakaotechbootcamp.community.config.annotation.UnitTest;
import com.kakaotechbootcamp.community.domain.post.policy.ViewCountPolicy;
import com.kakaotechbootcamp.community.domain.post.repository.PostRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@UnitTest
class PostViewServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private ViewCountPolicy viewCountPolicy;

    @InjectMocks
    private PostViewService postViewService;

    @Test
    @DisplayName("조회수 증가 정책을 통과하면 조회수가 증가한다")
    void incrementViewCount_whenPolicyShouldCount() {
        ViewContext context = ViewContext.builder()
                .memberId(1L)
                .ipAddress("127.0.0.1")
                .userAgent("user-agent")
                .build();
        given(viewCountPolicy.shouldCount(1L, context)).willReturn(true);

        postViewService.incrementViewCount(1L, context);

        verify(postRepository).incrementViewCount(1L);
    }

    @Test
    @DisplayName("조회수 증가 정책을 통과하지 못하면 조회수가 증가하지 않는다")
    void incrementViewCount_whenPolicyShouldNotCount() {
        ViewContext context = ViewContext.builder()
                .memberId(1L)
                .ipAddress("127.0.0.1")
                .userAgent("user-agent")
                .build();
        given(viewCountPolicy.shouldCount(1L, context)).willReturn(false);

        postViewService.incrementViewCount(1L, context);

        verify(postRepository, never()).incrementViewCount(1L);
    }
}
