package com.devon.techblog.application.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;

import com.devon.techblog.application.common.dto.response.PageResponse;
import com.devon.techblog.application.post.PostRequestFixture;
import com.devon.techblog.application.post.dto.request.PostCreateRequest;
import com.devon.techblog.application.post.dto.request.PostUpdateRequest;
import com.devon.techblog.application.post.dto.response.PostResponse;
import com.devon.techblog.application.post.dto.response.PostSummaryResponse;
import com.devon.techblog.common.exception.CustomException;
import com.devon.techblog.common.exception.code.MemberErrorCode;
import com.devon.techblog.common.exception.code.PostErrorCode;
import com.devon.techblog.config.annotation.UnitTest;
import com.devon.techblog.domain.common.policy.OwnershipPolicy;
import com.devon.techblog.domain.member.MemberFixture;
import com.devon.techblog.domain.member.entity.Member;
import com.devon.techblog.domain.member.repository.MemberRepository;
import com.devon.techblog.domain.post.PostFixture;
import com.devon.techblog.domain.post.PostQueryDtoFixture;
import com.devon.techblog.domain.post.dto.PostSearchCondition;
import com.devon.techblog.domain.post.dto.PostSummaryQueryDto;
import com.devon.techblog.domain.post.entity.Attachment;
import com.devon.techblog.domain.post.entity.Post;
import com.devon.techblog.domain.post.repository.AttachmentRepository;
import com.devon.techblog.domain.post.repository.PostLikeRepository;
import com.devon.techblog.domain.post.repository.PostRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@UnitTest
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private AttachmentRepository attachmentRepository;

    @Mock
    private OwnershipPolicy ownershipPolicy;

    @Mock
    private PostLikeRepository postLikeRepository;

    @InjectMocks
    private PostService postService;

    private Member member;
    private Post post;

    @BeforeEach
    void setUp() {
        member = MemberFixture.createWithId(1L);
        post = PostFixture.createWithId(1L, member);
    }

    @Test
    @DisplayName("게시글을 생성할 수 있다")
    void createPost_success() {
        PostCreateRequest request = PostRequestFixture.createRequest();
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(postRepository.save(any(Post.class))).willReturn(post);

        PostResponse response = postService.createPost(request, 1L);

        assertThat(response.title()).isEqualTo(PostFixture.DEFAULT_TITLE);
        assertThat(response.content()).isEqualTo(PostFixture.DEFAULT_CONTENT);
    }

    @Test
    @DisplayName("게시글 생성 시 이미지가 있으면 저장된다")
    void createPost_withImage() {
        PostCreateRequest request = PostRequestFixture.createRequest(PostFixture.DEFAULT_TITLE, PostFixture.DEFAULT_CONTENT, PostFixture.DEFAULT_IMAGE_URL);
        Attachment attachment = Attachment.create(post, PostFixture.DEFAULT_IMAGE_URL);
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(postRepository.save(any(Post.class))).willReturn(post);
        given(attachmentRepository.save(any(Attachment.class))).willReturn(attachment);

        PostResponse response = postService.createPost(request, 1L);

        assertThat(response.title()).isEqualTo(PostFixture.DEFAULT_TITLE);
        assertThat(response.imageUrl()).isNotNull();
    }

    @Test
    @DisplayName("게시글을 수정할 수 있다")
    void updatePost_success() {
        PostUpdateRequest request = PostRequestFixture.updateRequest();
        given(postRepository.findByIdWithMember(1L)).willReturn(Optional.of(post));
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(postRepository.save(post)).willReturn(post);
        given(attachmentRepository.findByPostId(1L)).willReturn(Optional.empty());

        postService.updatePost(1L, request, 1L);

        assertThat(post.getTitle()).isEqualTo(PostFixture.UPDATED_TITLE);
        assertThat(post.getContent()).isEqualTo(PostFixture.UPDATED_CONTENT);
    }

    @Test
    @DisplayName("게시글을 삭제할 수 있다")
    void deletePost_success() {
        given(postRepository.findByIdWithMember(1L)).willReturn(Optional.of(post));

        postService.deletePost(1L, 1L);

        assertThat(post.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("게시글 상세를 조회할 수 있다")
    void getPostDetails_success() {
        given(postRepository.findByIdWithMember(1L)).willReturn(Optional.of(post));
        given(attachmentRepository.findByPostId(1L)).willReturn(Optional.empty());
        given(postLikeRepository.existsByPostIdAndMemberId(1L, 1L)).willReturn(false);

        PostResponse response = postService.getPostDetails(1L, 1L);

        assertThat(response.postId()).isEqualTo(1L);
        assertThat(response.title()).isEqualTo(PostFixture.DEFAULT_TITLE);
    }

    @Test
    @DisplayName("존재하지 않는 게시글 조회 시 예외가 발생한다")
    void getPostDetails_notFound() {
        given(postRepository.findByIdWithMember(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> postService.getPostDetails(1L, 1L))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("게시글 목록을 페이지로 조회할 수 있다")
    void getPostPage_success() {
        Pageable pageable = PageRequest.of(0, 10);
        PostSummaryQueryDto dto = PostQueryDtoFixture.create();
        Page<PostSummaryQueryDto> page = new PageImpl<>(List.of(dto), pageable, 1);

        given(postRepository.searchPosts(any(PostSearchCondition.class), eq(pageable))).willReturn(page);

        PageResponse<PostSummaryResponse> response = postService.getPostPage(pageable);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().postId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("게시글 생성 시 회원이 존재하지 않으면 예외가 발생한다")
    void createPost_memberNotFound_throwsException() {
        PostCreateRequest request = PostRequestFixture.createRequest();
        given(memberRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> postService.createPost(request, 1L))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(MemberErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("게시글 수정 시 게시글이 존재하지 않으면 예외가 발생한다")
    void updatePost_postNotFound_throwsException() {
        PostUpdateRequest request = PostRequestFixture.updateRequest();
        given(postRepository.findByIdWithMember(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> postService.updatePost(1L, request, 1L))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(PostErrorCode.POST_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("게시글 수정 시 소유자가 아니면 예외가 발생한다")
    void updatePost_notOwner_throwsException() {
        PostUpdateRequest request = PostRequestFixture.updateRequest();
        Member otherMember = MemberFixture.createWithId(2L);
        given(postRepository.findByIdWithMember(1L)).willReturn(Optional.of(post));
        given(memberRepository.findById(2L)).willReturn(Optional.of(otherMember));
        doThrow(new CustomException(PostErrorCode.NO_PERMISSION))
                .when(ownershipPolicy).validateOwnership(1L, 2L);

        assertThatThrownBy(() -> postService.updatePost(1L, request, 2L))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(PostErrorCode.NO_PERMISSION.getMessage());
    }

    @Test
    @DisplayName("게시글 삭제 시 소유자가 아니면 예외가 발생한다")
    void deletePost_notOwner_throwsException() {
        given(postRepository.findByIdWithMember(1L)).willReturn(Optional.of(post));
        doThrow(new CustomException(PostErrorCode.NO_PERMISSION))
                .when(ownershipPolicy).validateOwnership(1L, 2L);

        assertThatThrownBy(() -> postService.deletePost(1L, 2L))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(PostErrorCode.NO_PERMISSION.getMessage());
    }

    @Test
    @DisplayName("게시글 조회 시 좋아요 여부를 확인한다 - memberId가 있을 때")
    void getPostDetails_withMemberId_checksLiked() {
        given(postRepository.findByIdWithMember(1L)).willReturn(Optional.of(post));
        given(attachmentRepository.findByPostId(1L)).willReturn(Optional.empty());
        given(postLikeRepository.existsByPostIdAndMemberId(1L, 1L)).willReturn(true);

        PostResponse response = postService.getPostDetails(1L, 1L);

        assertThat(response.isLiked()).isTrue();
    }

    @Test
    @DisplayName("게시글 조회 시 좋아요 여부를 확인한다 - memberId가 null일 때")
    void getPostDetails_withoutMemberId_isLikedFalse() {
        given(postRepository.findByIdWithMember(1L)).willReturn(Optional.of(post));
        given(attachmentRepository.findByPostId(1L)).willReturn(Optional.empty());

        PostResponse response = postService.getPostDetails(1L, null);

        assertThat(response.isLiked()).isFalse();
    }

    @Test
    @DisplayName("태그로 게시글을 필터링하여 조회할 수 있다")
    void getPostPageByTags_success() {
        Pageable pageable = PageRequest.of(0, 10);
        List<String> tags = List.of("Java", "Spring");
        PostSummaryQueryDto dto = PostQueryDtoFixture.create();
        Page<PostSummaryQueryDto> page = new PageImpl<>(List.of(dto), pageable, 1);

        given(postRepository.searchPosts(any(PostSearchCondition.class), eq(pageable))).willReturn(page);

        PageResponse<PostSummaryResponse> response = postService.getPostPageByTags(tags, pageable);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().postId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("태그로 게시글 조회 시 결과가 없으면 빈 리스트를 반환한다")
    void getPostPageByTags_noResults_returnsEmptyList() {
        Pageable pageable = PageRequest.of(0, 10);
        List<String> tags = List.of("NonExistentTag");
        Page<PostSummaryQueryDto> page = new PageImpl<>(List.of(), pageable, 0);

        given(postRepository.searchPosts(any(PostSearchCondition.class), eq(pageable))).willReturn(page);

        PageResponse<PostSummaryResponse> response = postService.getPostPageByTags(tags, pageable);

        assertThat(response.items()).isEmpty();
        assertThat(response.totalElements()).isZero();
    }
}
