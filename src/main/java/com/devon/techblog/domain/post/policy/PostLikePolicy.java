package com.devon.techblog.domain.post.policy;

import com.devon.techblog.common.exception.CustomException;
import com.devon.techblog.common.exception.code.PostErrorCode;
import com.devon.techblog.domain.post.repository.PostLikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PostLikePolicy {

    private final PostLikeRepository postLikeRepository;

    /**
     * 좋아요를 누를 수 있는지 검증
     */
    public void validateCanLike(Long postId, Long memberId) {
        if (postLikeRepository.existsByPostIdAndMemberId(postId, memberId)) {
            throw new CustomException(PostErrorCode.ALREADY_LIKED);
        }
    }

    /**
     * 좋아요를 취소할 수 있는지 검증
     */
    public void validateCanUnlike(Long postId, Long memberId) {
        if (!postLikeRepository.existsByPostIdAndMemberId(postId, memberId)) {
            throw new CustomException(PostErrorCode.LIKE_NOT_FOUND);
        }
    }
}
