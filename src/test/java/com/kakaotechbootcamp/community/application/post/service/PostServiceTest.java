package com.kakaotechbootcamp.community.application.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.kakaotechbootcamp.community.application.post.dto.request.PostCreateRequest;
import com.kakaotechbootcamp.community.application.post.dto.request.PostUpdateRequest;
import com.kakaotechbootcamp.community.application.post.dto.response.PostResponse;
import com.kakaotechbootcamp.community.common.exception.CustomException;
import com.kakaotechbootcamp.community.common.exception.code.MemberErrorCode;
import com.kakaotechbootcamp.community.common.exception.code.PostErrorCode;
import com.kakaotechbootcamp.community.config.UnitTest;
import com.kakaotechbootcamp.community.domain.common.policy.OwnershipPolicy;
import com.kakaotechbootcamp.community.domain.member.entity.Member;
import com.kakaotechbootcamp.community.domain.member.repository.MemberRepository;
import com.kakaotechbootcamp.community.domain.post.entity.Attachment;
import com.kakaotechbootcamp.community.domain.post.entity.Post;
import com.kakaotechbootcamp.community.domain.post.repository.AttachmentRepository;
import com.kakaotechbootcamp.community.domain.post.repository.PostLikeRepository;
import com.kakaotechbootcamp.community.domain.post.repository.PostRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

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

    @Test
    @DisplayName("게시글 생성 성공 - 첨부파일 없이")
    void createPost_WithoutAttachment_Success() {
        Long memberId = 1L;
        PostCreateRequest request = new PostCreateRequest(
                "테스트 제목",
                "테스트 내용",
                null
        );

        Member member = Member.create("test@email.com", "password", "tester");
        Post post = Post.create(member, request.title(), request.content());

        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(postRepository.save(any(Post.class))).willReturn(post);

        PostResponse response = postService.createPost(request, memberId);

        assertThat(response).isNotNull();
        assertThat(response.title()).isEqualTo("테스트 제목");
        assertThat(response.content()).isEqualTo("테스트 내용");
        verify(memberRepository).findById(memberId);
        verify(postRepository).save(any(Post.class));
        verify(attachmentRepository, never()).save(any(Attachment.class));
    }

    @Test
    @DisplayName("게시글 생성 성공 - 첨부파일과 함께")
    void createPost_WithAttachment_Success() {
        Long memberId = 1L;
        String imageUrl = "https://example.com/image.jpg";
        PostCreateRequest request = new PostCreateRequest(
                "테스트 제목",
                "테스트 내용",
                imageUrl
        );

        Member member = Member.create("test@email.com", "password", "tester");
        Post post = Post.create(member, request.title(), request.content());
        Attachment attachment = Attachment.create(post, imageUrl);

        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(postRepository.save(any(Post.class))).willReturn(post);
        given(attachmentRepository.save(any(Attachment.class))).willReturn(attachment);

        PostResponse response = postService.createPost(request, memberId);

        assertThat(response).isNotNull();
        assertThat(response.imageUrl()).isEqualTo(imageUrl);
        verify(memberRepository).findById(memberId);
        verify(postRepository).save(any(Post.class));
        verify(attachmentRepository).save(any(Attachment.class));
    }

    @Test
    @DisplayName("게시글 생성 실패 - 존재하지 않는 회원")
    void createPost_MemberNotFound_ThrowsException() {
        Long memberId = 999L;
        PostCreateRequest request = new PostCreateRequest(
                "테스트 제목",
                "테스트 내용",
                null
        );

        given(memberRepository.findById(memberId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> postService.createPost(request, memberId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", MemberErrorCode.USER_NOT_FOUND);

        verify(memberRepository).findById(memberId);
        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    @DisplayName("게시글 수정 성공")
    void updatePost_Success() {
        Long postId = 1L;
        Long memberId = 1L;
        PostUpdateRequest request = new PostUpdateRequest(
                "수정된 제목",
                "수정된 내용",
                null
        );

        Member member = mock(Member.class);
        given(member.getId()).willReturn(memberId);

        Post post = Post.create(member, "원본 제목", "원본 내용");

        given(postRepository.findByIdWithMember(postId)).willReturn(Optional.of(post));
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(postRepository.save(any(Post.class))).willReturn(post);
        given(attachmentRepository.findByPostId(postId)).willReturn(Optional.empty());

        PostResponse response = postService.updatePost(postId, request, memberId);

        assertThat(response).isNotNull();
        assertThat(response.title()).isEqualTo("수정된 제목");
        assertThat(response.content()).isEqualTo("수정된 내용");
        verify(postRepository).findByIdWithMember(postId);
        verify(ownershipPolicy).validateOwnership(memberId, memberId);
        verify(postRepository).save(any(Post.class));
    }

    @Test
    @DisplayName("게시글 수정 실패 - 존재하지 않는 게시글")
    void updatePost_PostNotFound_ThrowsException() {
        Long postId = 999L;
        Long memberId = 1L;
        PostUpdateRequest request = new PostUpdateRequest(
                "수정된 제목",
                "수정된 내용",
                null
        );

        given(postRepository.findByIdWithMember(postId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> postService.updatePost(postId, request, memberId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", PostErrorCode.POST_NOT_FOUND);

        verify(postRepository).findByIdWithMember(postId);
        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    @DisplayName("게시글 삭제 성공")
    void deletePost_Success() {
        Long postId = 1L;
        Long memberId = 1L;

        Member member = mock(Member.class);
        given(member.getId()).willReturn(memberId);

        Post post = Post.create(member, "제목", "내용");

        given(postRepository.findByIdWithMember(postId)).willReturn(Optional.of(post));
        given(postRepository.save(any(Post.class))).willReturn(post);

        postService.deletePost(postId, memberId);

        assertThat(post.isDeleted()).isTrue();
        verify(postRepository).findByIdWithMember(postId);
        verify(ownershipPolicy).validateOwnership(memberId, memberId);
        verify(postRepository).save(post);
    }

    @Test
    @DisplayName("게시글 조회 성공 - 좋아요 하지 않은 경우")
    void getPostDetails_NotLiked_Success() {
        Long postId = 1L;
        Long memberId = 1L;

        Member member = Member.create("test@email.com", "password", "tester");
        Post post = Post.create(member, "제목", "내용");

        given(postRepository.findByIdWithMember(postId)).willReturn(Optional.of(post));
        given(attachmentRepository.findByPostId(postId)).willReturn(Optional.empty());
        given(postLikeRepository.existsByPostIdAndMemberId(postId, memberId)).willReturn(false);

        PostResponse response = postService.getPostDetails(postId, memberId);

        assertThat(response).isNotNull();
        assertThat(response.postId()).isEqualTo(post.getId());
        assertThat(response.title()).isEqualTo("제목");
        assertThat(response.isLiked()).isFalse();
        verify(postRepository).findByIdWithMember(postId);
        verify(postLikeRepository).existsByPostIdAndMemberId(postId, memberId);
    }

    @Test
    @DisplayName("게시글 조회 성공 - 좋아요 한 경우")
    void getPostDetails_Liked_Success() {
        Long postId = 1L;
        Long memberId = 1L;

        Member member = Member.create("test@email.com", "password", "tester");
        Post post = Post.create(member, "제목", "내용");

        given(postRepository.findByIdWithMember(postId)).willReturn(Optional.of(post));
        given(attachmentRepository.findByPostId(postId)).willReturn(Optional.empty());
        given(postLikeRepository.existsByPostIdAndMemberId(postId, memberId)).willReturn(true);

        PostResponse response = postService.getPostDetails(postId, memberId);

        assertThat(response).isNotNull();
        assertThat(response.isLiked()).isTrue();
        verify(postLikeRepository).existsByPostIdAndMemberId(postId, memberId);
    }

    @Test
    @DisplayName("게시글 조회 성공 - 비로그인 사용자")
    void getPostDetails_NotLoggedIn_Success() {
        Long postId = 1L;

        Member member = Member.create("test@email.com", "password", "tester");
        Post post = Post.create(member, "제목", "내용");

        given(postRepository.findByIdWithMember(postId)).willReturn(Optional.of(post));
        given(attachmentRepository.findByPostId(postId)).willReturn(Optional.empty());

        PostResponse response = postService.getPostDetails(postId, null);

        assertThat(response).isNotNull();
        assertThat(response.isLiked()).isFalse();
        verify(postRepository).findByIdWithMember(postId);
        verify(postLikeRepository, never()).existsByPostIdAndMemberId(anyLong(), anyLong());
    }

    @Test
    @DisplayName("게시글 조회 실패 - 존재하지 않는 게시글")
    void getPostDetails_PostNotFound_ThrowsException() {
        Long postId = 999L;

        given(postRepository.findByIdWithMember(postId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> postService.getPostDetails(postId, null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", PostErrorCode.POST_NOT_FOUND);

        verify(postRepository).findByIdWithMember(postId);
    }
}
