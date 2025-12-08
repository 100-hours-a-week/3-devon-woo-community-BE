package com.devon.techblog.domain.post.policy;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.devon.techblog.common.exception.BusinessException;
import com.devon.techblog.config.annotation.UnitTest;
import com.devon.techblog.domain.post.repository.PostLikeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@UnitTest
class PostLikePolicyTest {

    @Mock
    private PostLikeRepository postLikeRepository;

    @InjectMocks
    private PostLikePolicy postLikePolicy;

    @Test
    @DisplayName("좋아요를 누르지 않은 상태면 좋아요를 누를 수 있다")
    void validateCanLike_success() {
        given(postLikeRepository.existsByPostIdAndMemberId(1L, 1L)).willReturn(false);

        postLikePolicy.validateCanLike(1L, 1L);
    }

    @Test
    @DisplayName("이미 좋아요를 누른 상태면 예외가 발생한다")
    void validateCanLike_alreadyLiked() {
        given(postLikeRepository.existsByPostIdAndMemberId(1L, 1L)).willReturn(true);

        assertThatThrownBy(() -> postLikePolicy.validateCanLike(1L, 1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("좋아요를 누른 상태면 좋아요를 취소할 수 있다")
    void validateCanUnlike_success() {
        given(postLikeRepository.existsByPostIdAndMemberId(1L, 1L)).willReturn(true);

        postLikePolicy.validateCanUnlike(1L, 1L);
    }

    @Test
    @DisplayName("좋아요를 누르지 않은 상태면 좋아요 취소 시 예외가 발생한다")
    void validateCanUnlike_notLiked() {
        given(postLikeRepository.existsByPostIdAndMemberId(1L, 1L)).willReturn(false);

        assertThatThrownBy(() -> postLikePolicy.validateCanUnlike(1L, 1L))
                .isInstanceOf(BusinessException.class);
    }
}
