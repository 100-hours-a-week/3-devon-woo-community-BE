package com.devon.techblog.application.post.service;

import com.devon.techblog.common.exception.CustomException;
import com.devon.techblog.common.exception.code.MemberErrorCode;
import com.devon.techblog.common.exception.code.PostErrorCode;
import com.devon.techblog.domain.member.entity.Member;
import com.devon.techblog.domain.member.repository.MemberRepository;
import com.devon.techblog.domain.post.entity.Post;
import com.devon.techblog.domain.post.entity.PostLike;
import com.devon.techblog.domain.post.policy.PostLikePolicy;
import com.devon.techblog.domain.post.repository.PostLikeRepository;
import com.devon.techblog.domain.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostLikeService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final MemberRepository memberRepository;
    private final PostLikePolicy postLikePolicy;

    /**
     * 게시글 좋아요
     */
    @Transactional
    public void likePost(Long postId, Long memberId) {
        Post post = postRepository.findByIdWithMember(postId)
                .orElseThrow(() -> new CustomException(PostErrorCode.POST_NOT_FOUND));

        if (!memberRepository.existsById(memberId)) {
            throw new CustomException(MemberErrorCode.USER_NOT_FOUND);
        }

        postLikePolicy.validateCanLike(postId, memberId);

        Member member = memberRepository.getReferenceById(memberId);
        postLikeRepository.save(PostLike.create(post, member));
        postRepository.incrementLikeCount(postId);
    }

    /**
     * 게시글 좋아요 취소
     */
    @Transactional
    public void unlikePost(Long postId, Long memberId) {
        if(!postRepository.existsById(postId)){
            throw new CustomException(PostErrorCode.POST_NOT_FOUND);
        }
        postLikePolicy.validateCanUnlike(postId, memberId);

        postLikeRepository.deleteByPostIdAndMemberId(postId, memberId);
        postRepository.decrementLikeCount(postId);
    }

}
