package com.kakaotechbootcamp.community.domain.post.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.kakaotechbootcamp.community.config.annotation.RepositoryJpaTest;
import com.kakaotechbootcamp.community.domain.member.MemberFixture;
import com.kakaotechbootcamp.community.domain.member.entity.Member;
import com.kakaotechbootcamp.community.domain.member.repository.MemberRepository;
import com.kakaotechbootcamp.community.domain.post.CommentFixture;
import com.kakaotechbootcamp.community.domain.post.PostFixture;
import com.kakaotechbootcamp.community.domain.post.entity.Comment;
import com.kakaotechbootcamp.community.domain.post.entity.Post;
import jakarta.transaction.Transactional;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@RepositoryJpaTest
@Transactional
class CommentRepositoryTest {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private MemberRepository memberRepository;

    private Member member;
    private Post post;

    @BeforeEach
    void setUp() {
        member = memberRepository.save(MemberFixture.create());
        post = postRepository.save(PostFixture.create(member));
    }

    @AfterEach
    void tearDown() {
        commentRepository.deleteAll();
        postRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("댓글을 저장하고 조회할 수 있다")
    void saveAndFind() {
        Comment comment = CommentFixture.create(member, post);
        Comment saved = commentRepository.save(comment);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getContent()).isEqualTo(CommentFixture.DEFAULT_CONTENT);
        assertThat(saved.getMember().getId()).isEqualTo(member.getId());
        assertThat(saved.getPost().getId()).isEqualTo(post.getId());
    }

    @Test
    @DisplayName("회원 정보와 함께 댓글을 조회할 수 있다")
    void findByIdWithMember() {
        Comment comment = commentRepository.save(CommentFixture.create(member, post));

        Comment found = commentRepository.findByIdWithMember(comment.getId()).orElseThrow();

        assertThat(found.getId()).isEqualTo(comment.getId());
        assertThat(found.getMember().getId()).isEqualTo(member.getId());
        assertThat(found.getMember().getNickname()).isEqualTo("tester");
    }

    @Test
    @DisplayName("댓글 ID로 게시글 ID를 조회할 수 있다")
    void findPostIdByCommentId() {
        Comment comment = commentRepository.save(CommentFixture.create(member, post));

        Long postId = commentRepository.findPostIdByCommentId(comment.getId()).orElseThrow();

        assertThat(postId).isEqualTo(post.getId());
    }

    @Test
    @DisplayName("존재하지 않는 댓글 ID로 게시글 ID 조회 시 빈 값을 반환한다")
    void findPostIdByCommentId_notFound() {
        assertThat(commentRepository.findPostIdByCommentId(999L)).isEmpty();
    }

    @Test
    @DisplayName("회원 ID로 게시글 정보와 함께 댓글을 조회할 수 있다")
    void findByMemberIdWithPost() {
        commentRepository.save(CommentFixture.create(member, post, "첫 번째 댓글"));
        commentRepository.save(CommentFixture.create(member, post, "두 번째 댓글"));

        List<Comment> comments = commentRepository.findByMemberIdWithPost(member.getId());

        assertThat(comments).hasSize(2);
    }

    @Test
    @DisplayName("회원 ID로 댓글 조회 시 생성일 역순으로 정렬된다")
    void findByMemberIdWithPost_orderedByCreatedAtDesc() {
        commentRepository.save(CommentFixture.create(member, post, "오래된 댓글"));
        commentRepository.save(CommentFixture.create(member, post, "최신 댓글"));

        List<Comment> comments = commentRepository.findByMemberIdWithPost(member.getId());

        assertThat(comments).hasSize(2);
    }

    @Test
    @DisplayName("다른 회원의 댓글은 조회되지 않는다")
    void findByMemberIdWithPost_onlyOwnComments() {
        Member otherMember = memberRepository.save(MemberFixture.create("other@test.com", "password", "other"));
        commentRepository.save(CommentFixture.create(member, post, "내 댓글"));
        commentRepository.save(CommentFixture.create(otherMember, post, "다른 사람 댓글"));

        List<Comment> comments = commentRepository.findByMemberIdWithPost(member.getId());

        assertThat(comments).hasSize(1);
        assertThat(comments.get(0).getContent()).isEqualTo("내 댓글");
    }
}
