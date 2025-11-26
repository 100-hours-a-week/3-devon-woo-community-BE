package com.devon.techblog.application.post.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.devon.techblog.common.exception.CustomException;
import com.devon.techblog.common.exception.code.PostErrorCode;
import com.devon.techblog.config.annotation.UnitTest;
import com.devon.techblog.domain.member.MemberFixture;
import com.devon.techblog.domain.member.entity.Member;
import com.devon.techblog.domain.member.repository.MemberRepository;
import com.devon.techblog.domain.post.PostFixture;
import com.devon.techblog.domain.post.entity.Post;
import com.devon.techblog.domain.post.entity.PostLike;
import com.devon.techblog.domain.post.policy.PostLikePolicy;
import com.devon.techblog.domain.post.repository.PostLikeRepository;
import com.devon.techblog.domain.post.repository.PostRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@UnitTest
class PostLikeServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostLikeRepository postLikeRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PostLikePolicy postLikePolicy;

    @InjectMocks
    private PostLikeService postLikeService;

    private Member member;
    private Post post;

    @BeforeEach
    void setUp() {
        member = MemberFixture.createWithId(1L);
        post = PostFixture.createWithId(1L, member);
    }

    @Test
    @DisplayName("게시글에 좋아요를 할 수 있다")
    void likePost_success() {
        given(postRepository.findByIdWithMember(1L)).willReturn(Optional.of(post));
        given(memberRepository.existsById(1L)).willReturn(true);
        given(memberRepository.getReferenceById(1L)).willReturn(member);
        doNothing().when(postLikePolicy).validateCanLike(1L, 1L);

        postLikeService.likePost(1L, 1L);

        verify(postLikePolicy).validateCanLike(1L, 1L);
        verify(postLikeRepository).save(any(PostLike.class));
        verify(postRepository).incrementLikeCount(1L);
    }

    @Test
    @DisplayName("존재하지 않는 게시글에 좋아요 시 예외가 발생한다")
    void likePost_postNotFound() {
        given(postRepository.findByIdWithMember(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> postLikeService.likePost(1L, 1L))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("존재하지 않는 회원이 좋아요 시 예외가 발생한다")
    void likePost_memberNotFound() {
        given(postRepository.findByIdWithMember(1L)).willReturn(Optional.of(post));
        given(memberRepository.existsById(1L)).willReturn(false);

        assertThatThrownBy(() -> postLikeService.likePost(1L, 1L))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("이미 좋아요한 게시글에 좋아요 시 예외가 발생한다")
    void likePost_alreadyLiked() {
        given(postRepository.findByIdWithMember(1L)).willReturn(Optional.of(post));
        given(memberRepository.existsById(1L)).willReturn(true);
        doThrow(new CustomException(PostErrorCode.ALREADY_LIKED)).when(postLikePolicy).validateCanLike(1L, 1L);

        assertThatThrownBy(() -> postLikeService.likePost(1L, 1L))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("게시글 좋아요를 취소할 수 있다")
    void unlikePost_success() {
        given(postRepository.existsById(1L)).willReturn(true);
        doNothing().when(postLikePolicy).validateCanUnlike(1L, 1L);

        postLikeService.unlikePost(1L, 1L);

        verify(postLikePolicy).validateCanUnlike(1L, 1L);
        verify(postLikeRepository).deleteByPostIdAndMemberId(1L, 1L);
        verify(postRepository).decrementLikeCount(1L);
    }

    @Test
    @DisplayName("존재하지 않는 게시글의 좋아요 취소 시 예외가 발생한다")
    void unlikePost_postNotFound() {
        given(postRepository.existsById(1L)).willReturn(false);

        assertThatThrownBy(() -> postLikeService.unlikePost(1L, 1L))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("좋아요하지 않은 게시글의 좋아요 취소 시 예외가 발생한다")
    void unlikePost_notLiked() {
        given(postRepository.existsById(1L)).willReturn(true);
        doThrow(new CustomException(PostErrorCode.LIKE_NOT_FOUND)).when(postLikePolicy).validateCanUnlike(1L, 1L);

        assertThatThrownBy(() -> postLikeService.unlikePost(1L, 1L))
                .isInstanceOf(CustomException.class);
    }
}
