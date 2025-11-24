package com.kakaotechbootcamp.community.application.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.kakaotechbootcamp.community.application.common.dto.response.PageResponse;
import com.kakaotechbootcamp.community.application.post.dto.request.PostCreateRequest;
import com.kakaotechbootcamp.community.application.post.dto.request.PostUpdateRequest;
import com.kakaotechbootcamp.community.application.post.dto.response.PostResponse;
import com.kakaotechbootcamp.community.application.post.dto.response.PostSummaryResponse;
import com.kakaotechbootcamp.community.common.exception.CustomException;
import com.kakaotechbootcamp.community.common.exception.code.CommonErrorCode;
import com.kakaotechbootcamp.community.config.annotation.UnitTest;
import com.kakaotechbootcamp.community.domain.common.policy.OwnershipPolicy;
import com.kakaotechbootcamp.community.domain.member.entity.Member;
import com.kakaotechbootcamp.community.domain.member.repository.MemberRepository;
import com.kakaotechbootcamp.community.domain.post.dto.PostQueryDto;
import com.kakaotechbootcamp.community.domain.post.entity.Attachment;
import com.kakaotechbootcamp.community.domain.post.entity.Post;
import com.kakaotechbootcamp.community.domain.post.repository.AttachmentRepository;
import com.kakaotechbootcamp.community.domain.post.repository.PostLikeRepository;
import com.kakaotechbootcamp.community.domain.post.repository.PostRepository;
import java.time.Instant;
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
import org.springframework.test.util.ReflectionTestUtils;

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
        member = Member.create("user@test.com", "password123", "tester");
        ReflectionTestUtils.setField(member, "id", 1L);

        post = Post.create(member, "제목", "내용");
        ReflectionTestUtils.setField(post, "id", 1L);
    }

    @Test
    @DisplayName("게시글을 생성할 수 있다")
    void createPost_success() {
        PostCreateRequest request = new PostCreateRequest("제목", "내용", null);
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(postRepository.save(any(Post.class))).willReturn(post);

        PostResponse response = postService.createPost(request, 1L);

        assertThat(response.title()).isEqualTo("제목");
        assertThat(response.content()).isEqualTo("내용");
        verify(postRepository).save(any(Post.class));
    }

    @Test
    @DisplayName("게시글 생성 시 이미지가 있으면 저장된다")
    void createPost_withImage() {
        PostCreateRequest request = new PostCreateRequest("제목", "내용", "https://example.com/image.jpg");
        Attachment attachment = Attachment.create(post, "https://example.com/image.jpg");
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(postRepository.save(any(Post.class))).willReturn(post);
        given(attachmentRepository.save(any(Attachment.class))).willReturn(attachment);

        PostResponse response = postService.createPost(request, 1L);

        verify(attachmentRepository).save(any(Attachment.class));
        assertThat(response.title()).isEqualTo("제목");
    }

    @Test
    @DisplayName("게시글을 수정할 수 있다")
    void updatePost_success() {
        PostUpdateRequest request = new PostUpdateRequest("수정된제목", "수정된내용", null);
        given(postRepository.findByIdWithMember(1L)).willReturn(Optional.of(post));
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(postRepository.save(post)).willReturn(post);
        given(attachmentRepository.findByPostId(1L)).willReturn(Optional.empty());
        doNothing().when(ownershipPolicy).validateOwnership(1L, 1L);

        postService.updatePost(1L, request, 1L);

        verify(ownershipPolicy).validateOwnership(1L, 1L);
        verify(postRepository).save(post);
        assertThat(post.getTitle()).isEqualTo("수정된제목");
        assertThat(post.getContent()).isEqualTo("수정된내용");
    }

    @Test
    @DisplayName("게시글 수정 시 권한이 없으면 예외가 발생한다")
    void updatePost_unauthorized() {
        PostUpdateRequest request = new PostUpdateRequest("수정된제목", "수정된내용", null);
        Member otherMember = Member.create("other@test.com", "password", "other");
        ReflectionTestUtils.setField(otherMember, "id", 2L);

        given(postRepository.findByIdWithMember(1L)).willReturn(Optional.of(post));
        given(memberRepository.findById(2L)).willReturn(Optional.of(otherMember));
        doThrow(new CustomException(CommonErrorCode.NO_PERMISSION)).when(ownershipPolicy).validateOwnership(1L, 2L);

        assertThatThrownBy(() -> postService.updatePost(1L, request, 2L))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("게시글을 삭제할 수 있다")
    void deletePost_success() {
        given(postRepository.findByIdWithMember(1L)).willReturn(Optional.of(post));
        doNothing().when(ownershipPolicy).validateOwnership(1L, 1L);

        postService.deletePost(1L, 1L);

        verify(ownershipPolicy).validateOwnership(1L, 1L);
        verify(postRepository).save(post);
        assertThat(post.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("게시글 삭제 시 권한이 없으면 예외가 발생한다")
    void deletePost_unauthorized() {
        given(postRepository.findByIdWithMember(1L)).willReturn(Optional.of(post));
        doThrow(new CustomException(CommonErrorCode.NO_PERMISSION)).when(ownershipPolicy).validateOwnership(1L, 2L);

        assertThatThrownBy(() -> postService.deletePost(1L, 2L))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("게시글 상세를 조회할 수 있다")
    void getPostDetails_success() {
        given(postRepository.findByIdWithMember(1L)).willReturn(Optional.of(post));
        given(attachmentRepository.findByPostId(1L)).willReturn(Optional.empty());
        given(postLikeRepository.existsByPostIdAndMemberId(1L, 1L)).willReturn(false);

        PostResponse response = postService.getPostDetails(1L, 1L);

        assertThat(response.postId()).isEqualTo(1L);
        assertThat(response.title()).isEqualTo("제목");
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
        PostQueryDto dto = new PostQueryDto(1L, "제목", Instant.now(), 0L, 0L, 0L, 1L, "tester", null);
        Page<PostQueryDto> page = new PageImpl<>(List.of(dto), pageable, 1);

        given(postRepository.findAllActiveWithMemberAsDto(pageable)).willReturn(page);

        PageResponse<PostSummaryResponse> response = postService.getPostPage(pageable);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().postId()).isEqualTo(1L);
    }
}
