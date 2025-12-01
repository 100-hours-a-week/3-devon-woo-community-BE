package com.devon.techblog.application.post.service;

import com.devon.techblog.application.common.dto.response.PageResponse;
import com.devon.techblog.application.post.dto.request.PostCreateRequest;
import com.devon.techblog.application.post.dto.request.PostUpdateRequest;
import com.devon.techblog.application.post.dto.response.PostResponse;
import com.devon.techblog.application.post.dto.response.PostSummaryResponse;
import com.devon.techblog.common.exception.CustomException;
import com.devon.techblog.common.exception.code.MemberErrorCode;
import com.devon.techblog.common.exception.code.PostErrorCode;
import com.devon.techblog.domain.common.policy.OwnershipPolicy;
import com.devon.techblog.domain.member.entity.Member;
import com.devon.techblog.domain.member.repository.MemberRepository;
import com.devon.techblog.domain.post.dto.PostQueryDto;
import com.devon.techblog.domain.post.entity.Attachment;
import com.devon.techblog.domain.post.entity.Post;
import com.devon.techblog.domain.post.repository.AttachmentRepository;
import com.devon.techblog.domain.post.repository.PostLikeRepository;
import com.devon.techblog.domain.post.repository.PostRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;
    private final MemberRepository memberRepository;
    private final AttachmentRepository attachmentRepository;
    private final OwnershipPolicy ownershipPolicy;
    private final PostLikeRepository postLikeRepository;
    private final TagService tagService;

    /**
     * 게시글 생성
     */
    @Transactional
    public PostResponse createPost(PostCreateRequest request, Long memberId) {
        Member member = findMemberById(memberId);

        Post post = Post.create(member, request.title(), request.content());

        if (request.summary() != null) {
            post.updateSummary(request.summary());
        }
        if (request.visibility() != null) {
            post.updateVisibility(request.visibility());
        }
        if (request.isDraft() != null && request.isDraft()) {
            post.markAsDraft();
        }
        else if (request.isDraft() != null) {
            post.publish();
        }
        if (request.commentsAllowed() != null) {
            post.setCommentsAllowed(request.commentsAllowed());
        }
        if (request.thumbnail() != null) {
            post.updateThumbnail(request.thumbnail());
        }
        if (request.image() != null) {
            post.updateImageUrl(request.image());
        }

        Post savedPost = postRepository.save(post);

        if (request.tags() != null && !request.tags().isEmpty()) {
            tagService.updatePostTags(savedPost, request.tags());
        }

        Attachment savedAttachment = Optional.ofNullable(request.image())
                .map(img -> attachmentRepository.save(Attachment.create(savedPost, img)))
                .orElse(null);

        return PostResponse.of(savedPost, member, savedAttachment);
    }

    /**
     * 게시글 수정
     */
    @Transactional
    public PostResponse updatePost(Long postId, PostUpdateRequest request, Long memberId) {
        Post post = findByIdWithMember(postId);
        Member member = findMemberById(memberId);

        ownershipPolicy.validateOwnership(post.getMember().getId(), memberId);

        if (request.title() != null || request.content() != null) {
            post.updatePost(
                    request.title() != null ? request.title() : post.getTitle(),
                    request.content() != null ? request.content() : post.getContent()
            );
        }

        if (request.summary() != null) {
            post.updateSummary(request.summary());
        }
        if (request.tags() != null) {
            tagService.updatePostTags(post, request.tags());
        }
        if (request.visibility() != null) {
            post.updateVisibility(request.visibility());
        }
        if (request.commentsAllowed() != null) {
            post.setCommentsAllowed(request.commentsAllowed());
        }
        if (request.thumbnail() != null) {
            post.updateThumbnail(request.thumbnail());
        }
        if (request.image() != null) {
            post.updateImageUrl(request.image());
        }

        Post savedPost = postRepository.save(post);

        Attachment attachment = Optional.ofNullable(request.image())
                .map(img -> attachmentRepository.save(Attachment.create(savedPost, img)))
                .orElseGet(() -> attachmentRepository.findByPostId(postId).orElse(null));

        return PostResponse.of(savedPost, member, attachment);
    }

    /**
     * 게시글 삭제
     */
    @Transactional
    public void deletePost(Long postId, Long memberId) {
        Post post = findByIdWithMember(postId);
        ownershipPolicy.validateOwnership(post.getMember().getId(), memberId);

        post.delete();
        postRepository.save(post);
    }

    /**
     * 게시글 조회
     */
    @Transactional(readOnly = true)
    public PostResponse getPostDetails(Long postId, Long memberId) {
        Post post = findByIdWithMember(postId);

        Member member = post.getMember();
        Attachment attachment = attachmentRepository.findByPostId(postId)
                .orElse(null);

        boolean isLiked = false;
        if(memberId != null && postLikeRepository.existsByPostIdAndMemberId(postId, memberId)){
            isLiked = true;
        }

        return PostResponse.of(post, member, attachment, isLiked);
    }

    /**
     * 게시글 페이지 조회 (+페이징 및 정렬)
     */
    @Transactional(readOnly = true)
    public PageResponse<PostSummaryResponse> getPostPage(Pageable pageable) {
        Page<PostQueryDto> postDtoPage = postRepository.findAllActiveWithMemberAsDto(pageable);

        List<PostSummaryResponse> postSummaries = postDtoPage.getContent().stream()
                .map(PostSummaryResponse::fromDto)
                .toList();

        return PageResponse.of(postSummaries, postDtoPage);
    }

    /**
     * 태그로 게시글 필터링 조회 (+페이징 및 정렬)
     */
    @Transactional(readOnly = true)
    public PageResponse<PostSummaryResponse> getPostPageByTags(List<String> tags, Pageable pageable) {
        Page<PostQueryDto> postDtoPage = postRepository.findByTagsIn(tags, pageable);

        List<PostSummaryResponse> postSummaries = postDtoPage.getContent().stream()
                .map(PostSummaryResponse::fromDto)
                .toList();

        return PageResponse.of(postSummaries, postDtoPage);
    }

    private Post findByIdWithMember(Long postId) {
        return postRepository.findByIdWithMember(postId)
                .orElseThrow(() -> new CustomException(PostErrorCode.POST_NOT_FOUND));
    }

    private Member findMemberById(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(MemberErrorCode.USER_NOT_FOUND));
    }
}
