package com.kakaotechbootcamp.community.application.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.kakaotechbootcamp.community.application.comment.dto.request.CommentCreateRequest;
import com.kakaotechbootcamp.community.application.comment.dto.request.CommentUpdateRequest;
import com.kakaotechbootcamp.community.application.comment.dto.response.CommentResponse;
import com.kakaotechbootcamp.community.application.common.dto.response.PageResponse;
import com.kakaotechbootcamp.community.common.exception.CustomException;
import com.kakaotechbootcamp.community.config.annotation.UnitTest;
import com.kakaotechbootcamp.community.domain.common.policy.OwnershipPolicy;
import com.kakaotechbootcamp.community.domain.member.entity.Member;
import com.kakaotechbootcamp.community.domain.member.repository.MemberRepository;
import com.kakaotechbootcamp.community.domain.post.dto.CommentQueryDto;
import com.kakaotechbootcamp.community.domain.post.entity.Comment;
import com.kakaotechbootcamp.community.domain.post.entity.Post;
import com.kakaotechbootcamp.community.domain.post.repository.CommentRepository;
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
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private OwnershipPolicy ownershipPolicy;

    @InjectMocks
    private CommentService commentService;

    private Member member;
    private Post post;
    private Comment comment;

    @BeforeEach
    void setUp() {
        member = Member.create("user@test.com", "password123", "tester");
        ReflectionTestUtils.setField(member, "id", 1L);

        post = Post.create(member, "제목", "내용");
        ReflectionTestUtils.setField(post, "id", 1L);

        comment = Comment.create(member, post, "댓글내용");
        ReflectionTestUtils.setField(comment, "id", 1L);
    }

    @Test
    @DisplayName("댓글을 작성할 수 있다")
    void createComment_success() {
        CommentCreateRequest request = new CommentCreateRequest("댓글내용");
        given(postRepository.existsById(1L)).willReturn(true);
        given(postRepository.getReferenceById(1L)).willReturn(post);
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(commentRepository.save(any(Comment.class))).willReturn(comment);

        CommentResponse response = commentService.createComment(1L, request, 1L);

        assertThat(response.content()).isEqualTo("댓글내용");
        verify(commentRepository).save(any(Comment.class));
        verify(postRepository).incrementCommentCount(1L);
    }

    @Test
    @DisplayName("존재하지 않는 게시글에 댓글 작성 시 예외가 발생한다")
    void createComment_postNotFound() {
        CommentCreateRequest request = new CommentCreateRequest("댓글내용");
        given(postRepository.existsById(1L)).willReturn(false);

        assertThatThrownBy(() -> commentService.createComment(1L, request, 1L))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("댓글을 수정할 수 있다")
    void updateComment_success() {
        CommentUpdateRequest request = new CommentUpdateRequest("수정된댓글");
        given(commentRepository.findByIdWithMember(1L)).willReturn(Optional.of(comment));
        given(commentRepository.save(comment)).willReturn(comment);

        CommentResponse response = commentService.updateComment(1L, request, 1L);

        verify(ownershipPolicy).validateOwnership(1L, 1L);
        verify(commentRepository).save(comment);
        assertThat(comment.getContent()).isEqualTo("수정된댓글");
    }

    @Test
    @DisplayName("댓글을 삭제할 수 있다")
    void deleteComment_success() {
        given(commentRepository.findByIdWithMember(1L)).willReturn(Optional.of(comment));
        given(commentRepository.findPostIdByCommentId(1L)).willReturn(Optional.of(1L));

        commentService.deleteComment(1L, 1L);

        verify(ownershipPolicy).validateOwnership(1L, 1L);
        verify(commentRepository).deleteById(1L);
        verify(postRepository).decrementCommentCount(1L);
    }

    @Test
    @DisplayName("댓글 상세를 조회할 수 있다")
    void getCommentsDetails_success() {
        given(commentRepository.findByIdWithMember(1L)).willReturn(Optional.of(comment));

        CommentResponse response = commentService.getCommentsDetails(1L);

        assertThat(response.commentId()).isEqualTo(1L);
        assertThat(response.content()).isEqualTo("댓글내용");
    }

    @Test
    @DisplayName("존재하지 않는 댓글 조회 시 예외가 발생한다")
    void getCommentsDetails_notFound() {
        given(commentRepository.findByIdWithMember(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.getCommentsDetails(1L))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("게시글의 댓글 목록을 페이지로 조회할 수 있다")
    void getCommentPageByPostId_success() {
        Pageable pageable = PageRequest.of(0, 10);
        CommentQueryDto dto = new CommentQueryDto(1L, 1L, "댓글내용", Instant.now(), Instant.now(), 1L, "tester", null);
        Page<CommentQueryDto> page = new PageImpl<>(List.of(dto), pageable, 1);

        given(postRepository.existsById(1L)).willReturn(true);
        given(commentRepository.findByPostIdWithMemberAsDto(1L, pageable)).willReturn(page);

        PageResponse<CommentResponse> response = commentService.getCommentPageByPostId(1L, pageable);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).commentId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("존재하지 않는 게시글의 댓글 목록 조회 시 예외가 발생한다")
    void getCommentPageByPostId_postNotFound() {
        Pageable pageable = PageRequest.of(0, 10);
        given(postRepository.existsById(1L)).willReturn(false);

        assertThatThrownBy(() -> commentService.getCommentPageByPostId(1L, pageable))
                .isInstanceOf(CustomException.class);
    }
}
