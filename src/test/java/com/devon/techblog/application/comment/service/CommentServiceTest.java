package com.devon.techblog.application.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.devon.techblog.application.comment.CommentRequestFixture;
import com.devon.techblog.application.comment.dto.request.CommentCreateRequest;
import com.devon.techblog.application.comment.dto.request.CommentUpdateRequest;
import com.devon.techblog.application.comment.dto.response.CommentResponse;
import com.devon.techblog.application.common.dto.response.PageResponse;
import com.devon.techblog.common.exception.CustomException;
import com.devon.techblog.common.exception.code.CommentErrorCode;
import com.devon.techblog.common.exception.code.CommonErrorCode;
import com.devon.techblog.common.exception.code.MemberErrorCode;
import com.devon.techblog.config.annotation.UnitTest;
import com.devon.techblog.domain.common.policy.OwnershipPolicy;
import com.devon.techblog.domain.member.MemberFixture;
import com.devon.techblog.domain.member.entity.Member;
import com.devon.techblog.domain.member.repository.MemberRepository;
import com.devon.techblog.domain.post.CommentFixture;
import com.devon.techblog.domain.post.PostFixture;
import com.devon.techblog.domain.post.dto.CommentQueryDto;
import com.devon.techblog.domain.post.entity.Comment;
import com.devon.techblog.domain.post.entity.Post;
import com.devon.techblog.domain.post.repository.CommentRepository;
import com.devon.techblog.domain.post.repository.PostRepository;
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

@UnitTest
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PostRepository postRepository;

    private CommentService commentService;

    private Member member;
    private Post post;
    private Comment comment;

    @BeforeEach
    void setUp() {
        // spy로 실제 객체 사용하기
        OwnershipPolicy ownershipPolicy = new OwnershipPolicy();
        commentService = new CommentService(commentRepository, memberRepository, postRepository, ownershipPolicy);

        member = MemberFixture.createWithId(1L);
        post = PostFixture.createWithId(1L, member);
        comment = CommentFixture.createWithId(1L, member, post);
    }

    @Test
    @DisplayName("댓글을 작성할 수 있다")
    void createComment_success() {
        CommentCreateRequest request = CommentRequestFixture.createRequest();
        given(postRepository.existsById(1L)).willReturn(true);
        given(postRepository.getReferenceById(1L)).willReturn(post);
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(commentRepository.save(any(Comment.class))).willReturn(comment);

        CommentResponse response = commentService.createComment(1L, request, 1L);

        assertThat(response.content()).isEqualTo(CommentFixture.DEFAULT_CONTENT);
    }

    @Test
    @DisplayName("존재하지 않는 게시글에 댓글 작성 시 예외가 발생한다")
    void createComment_postNotFound() {
        CommentCreateRequest request = CommentRequestFixture.createRequest();
        given(postRepository.existsById(1L)).willReturn(false);

        assertThatThrownBy(() -> commentService.createComment(1L, request, 1L))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("댓글을 수정할 수 있다")
    void updateComment_success() {
        CommentUpdateRequest request = CommentRequestFixture.updateRequest();
        given(commentRepository.findByIdWithMember(1L)).willReturn(Optional.of(comment));
        given(commentRepository.save(comment)).willReturn(comment);

        CommentResponse response = commentService.updateComment(1L, request, 1L);

        assertThat(comment.getContent()).isEqualTo(CommentFixture.UPDATED_CONTENT);
        assertThat(response.content()).isEqualTo(CommentFixture.UPDATED_CONTENT);
    }

    @Test
    @DisplayName("댓글을 삭제할 수 있다")
    void deleteComment_success() {
        given(commentRepository.findByIdWithMember(1L)).willReturn(Optional.of(comment));
        given(commentRepository.findPostIdByCommentId(1L)).willReturn(Optional.of(1L));

        commentService.deleteComment(1L, 1L);
    }

    @Test
    @DisplayName("댓글 상세를 조회할 수 있다")
    void getCommentsDetails_success() {
        given(commentRepository.findByIdWithMember(1L)).willReturn(Optional.of(comment));

        CommentResponse response = commentService.getCommentsDetails(1L);

        assertThat(response.commentId()).isEqualTo(1L);
        assertThat(response.content()).isEqualTo(CommentFixture.DEFAULT_CONTENT);
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
        CommentQueryDto dto = new CommentQueryDto(1L, 1L, CommentFixture.DEFAULT_CONTENT, Instant.now(), Instant.now(), 1L, "tester", null);
        Page<CommentQueryDto> page = new PageImpl<>(List.of(dto), pageable, 1);

        given(postRepository.existsById(1L)).willReturn(true);
        given(commentRepository.findByPostIdWithMemberAsDto(1L, pageable)).willReturn(page);

        PageResponse<CommentResponse> response = commentService.getCommentPageByPostId(1L, pageable);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().commentId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("존재하지 않는 게시글의 댓글 목록 조회 시 예외가 발생한다")
    void getCommentPageByPostId_postNotFound() {
        Pageable pageable = PageRequest.of(0, 10);
        given(postRepository.existsById(1L)).willReturn(false);

        assertThatThrownBy(() -> commentService.getCommentPageByPostId(1L, pageable))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("댓글 작성 시 회원이 존재하지 않으면 예외가 발생한다")
    void createComment_memberNotFound_throwsException() {
        CommentCreateRequest request = CommentRequestFixture.createRequest();
        given(postRepository.existsById(1L)).willReturn(true);
        given(postRepository.getReferenceById(1L)).willReturn(post);
        given(memberRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.createComment(1L, request, 1L))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(MemberErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("댓글 작성 시 댓글 카운트가 증가한다")
    void createComment_incrementsCommentCount() {
        CommentCreateRequest request = CommentRequestFixture.createRequest();
        given(postRepository.existsById(1L)).willReturn(true);
        given(postRepository.getReferenceById(1L)).willReturn(post);
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(commentRepository.save(any(Comment.class))).willReturn(comment);

        commentService.createComment(1L, request, 1L);

        verify(postRepository).incrementCommentCount(1L);
    }

    @Test
    @DisplayName("댓글 수정 시 댓글이 존재하지 않으면 예외가 발생한다")
    void updateComment_commentNotFound_throwsException() {
        CommentUpdateRequest request = CommentRequestFixture.updateRequest();
        given(commentRepository.findByIdWithMember(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.updateComment(1L, request, 1L))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(CommentErrorCode.COMMENT_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("댓글 수정 시 소유자가 아니면 예외가 발생한다")
    void updateComment_notOwner_throwsException() {
        CommentUpdateRequest request = CommentRequestFixture.updateRequest();
        given(commentRepository.findByIdWithMember(1L)).willReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.updateComment(1L, request, 2L))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(CommonErrorCode.NO_PERMISSION.getMessage());
    }

    @Test
    @DisplayName("댓글 삭제 시 댓글이 존재하지 않으면 예외가 발생한다")
    void deleteComment_commentNotFound_throwsException() {
        given(commentRepository.findByIdWithMember(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.deleteComment(1L, 1L))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(CommentErrorCode.COMMENT_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("댓글 삭제 시 postId를 찾을 수 없으면 예외가 발생한다")
    void deleteComment_postIdNotFound_throwsException() {
        given(commentRepository.findByIdWithMember(1L)).willReturn(Optional.of(comment));
        given(commentRepository.findPostIdByCommentId(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.deleteComment(1L, 1L))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(CommentErrorCode.COMMENT_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("댓글 삭제 시 댓글 카운트가 감소한다")
    void deleteComment_decrementsCommentCount() {
        given(commentRepository.findByIdWithMember(1L)).willReturn(Optional.of(comment));
        given(commentRepository.findPostIdByCommentId(1L)).willReturn(Optional.of(1L));

        commentService.deleteComment(1L, 1L);

        verify(postRepository).decrementCommentCount(1L);
    }

    @Test
    @DisplayName("댓글 삭제 시 소유자가 아니면 예외가 발생한다")
    void deleteComment_notOwner_throwsException() {
        given(commentRepository.findByIdWithMember(1L)).willReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.deleteComment(1L, 2L))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(CommentErrorCode.NO_PERMISSION.getMessage());
    }
}
