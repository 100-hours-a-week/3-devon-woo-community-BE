package com.kakaotechbootcamp.community.domain.post.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.kakaotechbootcamp.community.config.annotation.RepositoryJpaTest;
import com.kakaotechbootcamp.community.domain.member.entity.Member;
import com.kakaotechbootcamp.community.domain.member.repository.MemberRepository;
import com.kakaotechbootcamp.community.domain.post.entity.Post;
import com.kakaotechbootcamp.community.domain.post.PostFixture;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@RepositoryJpaTest
@Transactional
class PostRepositoryTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private MemberRepository memberRepository;

    private Member member;

    @BeforeEach
    void setUp() {
        member = memberRepository.save(Member.create("user@test.com", "password123", "tester"));
    }

    @AfterEach
    void tearDown() {
        postRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("게시글을 저장하고 조회할 수 있다")
    void saveAndFind() {
        Post post = Post.create(member, PostFixture.DEFAULT_TITLE, PostFixture.DEFAULT_CONTENT);
        Post saved = postRepository.save(post);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTitle()).isEqualTo(PostFixture.DEFAULT_TITLE);
        assertThat(saved.getContent()).isEqualTo(PostFixture.DEFAULT_CONTENT);
    }

    @Test
    @DisplayName("회원 정보와 함께 게시글을 조회할 수 있다")
    void findByIdWithMember() {
        Post post = postRepository.save(Post.create(member, PostFixture.DEFAULT_TITLE, PostFixture.DEFAULT_CONTENT));

        Post found = postRepository.findByIdWithMember(post.getId()).orElseThrow();

        assertThat(found.getId()).isEqualTo(post.getId());
        assertThat(found.getMember().getId()).isEqualTo(member.getId());
        assertThat(found.getMember().getNickname()).isEqualTo("tester");
    }

    @Test
    @DisplayName("삭제된 게시글은 findByIdWithMember로 조회되지 않는다")
    void findByIdWithMember_deletedPost() {
        Post post = postRepository.save(Post.create(member, PostFixture.DEFAULT_TITLE, PostFixture.DEFAULT_CONTENT));
        post.delete();
        postRepository.save(post);

        assertThat(postRepository.findByIdWithMember(post.getId())).isEmpty();
    }

    @Test
    @DisplayName("좋아요 수를 증가시킬 수 있다")
    void incrementLikeCount() {
        Post post = postRepository.save(Post.create(member, PostFixture.DEFAULT_TITLE, PostFixture.DEFAULT_CONTENT));
        Long postId = post.getId();

        int updated = postRepository.incrementLikeCount(postId);
        postRepository.flush();

        assertThat(updated).isEqualTo(1);
        Post found = postRepository.findById(postId).orElseThrow();
        assertThat(found.getLikeCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("좋아요 수를 감소시킬 수 있다")
    void decrementLikeCount() {
        Post post = postRepository.save(Post.create(member, PostFixture.DEFAULT_TITLE, PostFixture.DEFAULT_CONTENT));
        Long postId = post.getId();
        postRepository.incrementLikeCount(postId);
        postRepository.flush();

        int updated = postRepository.decrementLikeCount(postId);
        postRepository.flush();

        assertThat(updated).isEqualTo(1);
        Post found = postRepository.findById(postId).orElseThrow();
        assertThat(found.getLikeCount()).isEqualTo(0L);
    }

    @Test
    @DisplayName("좋아요 수가 0일 때 감소시키면 0을 유지한다")
    void decrementLikeCount_whenZero() {
        Post post = postRepository.save(Post.create(member, PostFixture.DEFAULT_TITLE, PostFixture.DEFAULT_CONTENT));
        Long postId = post.getId();

        postRepository.decrementLikeCount(postId);
        postRepository.flush();

        Post found = postRepository.findById(postId).orElseThrow();
        assertThat(found.getLikeCount()).isEqualTo(0L);
    }

    @Test
    @DisplayName("조회수를 증가시킬 수 있다")
    void incrementViewCount() {
        Post post = postRepository.save(Post.create(member, PostFixture.DEFAULT_TITLE, PostFixture.DEFAULT_CONTENT));
        Long postId = post.getId();

        int updated = postRepository.incrementViewCount(postId);
        postRepository.flush();

        assertThat(updated).isEqualTo(1);
        Post found = postRepository.findById(postId).orElseThrow();
        assertThat(found.getViewsCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("댓글 수를 증가시킬 수 있다")
    void incrementCommentCount() {
        Post post = postRepository.save(Post.create(member, PostFixture.DEFAULT_TITLE, PostFixture.DEFAULT_CONTENT));
        Long postId = post.getId();

        int updated = postRepository.incrementCommentCount(postId);
        postRepository.flush();

        assertThat(updated).isEqualTo(1);
        Post found = postRepository.findById(postId).orElseThrow();
        assertThat(found.getCommentCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("댓글 수를 감소시킬 수 있다")
    void decrementCommentCount() {
        Post post = postRepository.save(Post.create(member, PostFixture.DEFAULT_TITLE, PostFixture.DEFAULT_CONTENT));
        Long postId = post.getId();
        postRepository.incrementCommentCount(postId);
        postRepository.flush();

        int updated = postRepository.decrementCommentCount(postId);
        postRepository.flush();

        assertThat(updated).isEqualTo(1);
        Post found = postRepository.findById(postId).orElseThrow();
        assertThat(found.getCommentCount()).isEqualTo(0L);
    }

    @Test
    @DisplayName("댓글 수가 0일 때 감소시키면 0을 유지한다")
    void decrementCommentCount_whenZero() {
        Post post = postRepository.save(Post.create(member, PostFixture.DEFAULT_TITLE, PostFixture.DEFAULT_CONTENT));
        Long postId = post.getId();

        postRepository.decrementCommentCount(postId);
        postRepository.flush();

        Post found = postRepository.findById(postId).orElseThrow();
        assertThat(found.getCommentCount()).isEqualTo(0L);
    }
}
