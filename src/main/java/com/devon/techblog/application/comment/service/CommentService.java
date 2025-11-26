package com.devon.techblog.application.comment.service;

import com.devon.techblog.application.comment.dto.request.CommentCreateRequest;
import com.devon.techblog.application.comment.dto.request.CommentUpdateRequest;
import com.devon.techblog.application.comment.dto.response.CommentResponse;
import com.devon.techblog.application.common.dto.response.PageResponse;
import com.devon.techblog.common.exception.CustomException;
import com.devon.techblog.common.exception.code.CommentErrorCode;
import com.devon.techblog.common.exception.code.MemberErrorCode;
import com.devon.techblog.common.exception.code.PostErrorCode;
import com.devon.techblog.domain.common.policy.OwnershipPolicy;
import com.devon.techblog.domain.member.entity.Member;
import com.devon.techblog.domain.member.repository.MemberRepository;
import com.devon.techblog.domain.post.dto.CommentQueryDto;
import com.devon.techblog.domain.post.entity.Comment;
import com.devon.techblog.domain.post.entity.Post;
import com.devon.techblog.domain.post.repository.CommentRepository;
import com.devon.techblog.domain.post.repository.PostRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final MemberRepository memberRepository;
    private final PostRepository postRepository;
    private final OwnershipPolicy ownershipPolicy;

    /**
     * 댓글 작성
     */
    @Transactional
    public CommentResponse createComment(Long postId, CommentCreateRequest request, Long memberId) {
        validatePostExists(postId);
        Post post = postRepository.getReferenceById(postId);
        Member member = findMemberById(memberId);

        Comment comment = Comment.create(member, post, request.content());
        commentRepository.save(comment);

        postRepository.incrementCommentCount(postId);

        return CommentResponse.of(comment, member);
    }

    /**
     * 댓글 업데이튼
     */
    @Transactional
    public CommentResponse updateComment(Long commentId, CommentUpdateRequest request, Long requesterId) {
        Comment comment = findCommentByIdWithMember(commentId);
        Member member = comment.getMember();

        ownershipPolicy.validateOwnership(comment.getMember().getId(), requesterId);

        comment.updateContent(request.content());
        commentRepository.save(comment);

        return CommentResponse.of(comment, member);
    }

    /**
     * 댓글 삭제
     */
    @Transactional
    public void deleteComment(Long commentId, Long requesterId) {
        Comment comment = findCommentByIdWithMember(commentId);
        ownershipPolicy.validateOwnership(comment.getMember().getId(), requesterId);

        Long postId = commentRepository.findPostIdByCommentId(commentId)
                .orElseThrow(() -> new CustomException(CommentErrorCode.COMMENT_NOT_FOUND));

        commentRepository.deleteById(comment.getId());

        postRepository.decrementCommentCount(postId);
    }

    /**
     * 댓글 살세 조회
     */
    @Transactional(readOnly = true)
    public CommentResponse getCommentsDetails(Long commentId) {
        Comment comment = findCommentByIdWithMember(commentId);

        return CommentResponse.of(comment, comment.getMember());
    }

    /**
     * 게시글의 댓글 페이지 조회 (+페이징 및 정렬)
     */
    @Transactional(readOnly = true)
    public PageResponse<CommentResponse> getCommentPageByPostId(Long postId, Pageable pageable) {
        validatePostExists(postId);

        Page<CommentQueryDto> commentDtoPage = commentRepository.findByPostIdWithMemberAsDto(postId, pageable);

        List<CommentResponse> commentResponses = commentDtoPage.getContent().stream()
                .map(CommentResponse::of)
                .toList();

        return PageResponse.of(commentResponses, commentDtoPage);
    }

    private void validatePostExists(Long postId) {
        if (!postRepository.existsById(postId)) {
            throw new CustomException(PostErrorCode.POST_NOT_FOUND);
        }
    }

    private Comment findCommentByIdWithMember(Long commentId) {
        return commentRepository.findByIdWithMember(commentId)
                .orElseThrow(() -> new CustomException(CommentErrorCode.COMMENT_NOT_FOUND));
    }

    private Member findMemberById(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(MemberErrorCode.USER_NOT_FOUND));
    }
}
