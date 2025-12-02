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
import com.devon.techblog.domain.post.dto.PostSearchCondition;
import com.devon.techblog.domain.post.dto.PostSummaryQueryDto;
import com.devon.techblog.domain.post.entity.Attachment;
import com.devon.techblog.domain.post.entity.Post;
import com.devon.techblog.domain.post.repository.AttachmentRepository;
import com.devon.techblog.domain.post.repository.PostLikeRepository;
import com.devon.techblog.domain.post.repository.PostRepository;
import java.util.List;
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

        applyPostMutation(post, PostMutationData.fromCreateRequest(request));

        Post savedPost = postRepository.save(post);

        updateTags(savedPost, request.tags(), false);

        Attachment savedAttachment = saveAttachment(savedPost, request.image(), false);

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

        applyPostMutation(post, PostMutationData.fromUpdateRequest(request));
        updateTags(post, request.tags(), true);

        Post savedPost = postRepository.save(post);

        Attachment attachment = saveAttachment(savedPost, request.image(), true);

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
        Page<PostSummaryQueryDto> postDtoPage = postRepository.searchPosts(PostSearchCondition.empty(), pageable);

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
        Page<PostSummaryQueryDto> postDtoPage = postRepository.searchPosts(PostSearchCondition.forTags(tags), pageable);

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

    private void applyPostMutation(Post post, PostMutationData data) {
        if (data.summary() != null) {
            post.updateSummary(data.summary());
        }
        if (data.visibility() != null) {
            post.updateVisibility(data.visibility());
        }
        if (data.isDraft() != null) {
            if (data.isDraft()) {
                post.markAsDraft();
            } else {
                post.publish();
            }
        }
        if (data.commentsAllowed() != null) {
            post.setCommentsAllowed(data.commentsAllowed());
        }
        if (data.thumbnail() != null) {
            post.updateThumbnail(data.thumbnail());
        }
        if (data.image() != null) {
            post.updateImageUrl(data.image());
        }
    }

    private void updateTags(Post post, List<String> tags, boolean allowEmpty) {
        if (tags == null) {
            return;
        }
        if (!allowEmpty && tags.isEmpty()) {
            return;
        }
        tagService.updatePostTags(post, tags);
    }

    private Attachment saveAttachment(Post post, String imageUrl, boolean fallbackToExisting) {
        if (imageUrl != null) {
            return attachmentRepository.save(Attachment.create(post, imageUrl));
        }
        if (fallbackToExisting) {
            return attachmentRepository.findByPostId(post.getId()).orElse(null);
        }
        return null;
    }

    private record PostMutationData(
            String summary,
            String visibility,
            Boolean isDraft,
            Boolean commentsAllowed,
            String thumbnail,
            String image
    ) {

        private static PostMutationData fromCreateRequest(PostCreateRequest request) {
            return new PostMutationData(
                    request.summary(),
                    request.visibility(),
                    request.isDraft(),
                    request.commentsAllowed(),
                    request.thumbnail(),
                    request.image()
            );
        }

        private static PostMutationData fromUpdateRequest(PostUpdateRequest request) {
            return new PostMutationData(
                    request.summary(),
                    request.visibility(),
                    null,
                    request.commentsAllowed(),
                    request.thumbnail(),
                    request.image()
            );
        }
    }
}
