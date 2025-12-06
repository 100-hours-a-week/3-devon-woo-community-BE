package com.devon.techblog.domain.post.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.devon.techblog.config.annotation.RepositoryJpaTest;
import com.devon.techblog.domain.member.MemberFixture;
import com.devon.techblog.domain.member.entity.Member;
import com.devon.techblog.domain.member.repository.MemberRepository;
import com.devon.techblog.domain.post.PostFixture;
import com.devon.techblog.domain.post.entity.Post;
import com.devon.techblog.domain.post.entity.Series;
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

    @Autowired
    private SeriesRepository seriesRepository;

    private Member member;

    @BeforeEach
    void setUp() {
        member = memberRepository.save(MemberFixture.create());
    }

    @AfterEach
    void tearDown() {
        postRepository.deleteAll();
        seriesRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("게시글을 저장하고 조회할 수 있다")
    void saveAndFind() {
        Post post = PostFixture.create(member);
        Post saved = postRepository.save(post);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTitle()).isEqualTo(PostFixture.DEFAULT_TITLE);
        assertThat(saved.getContent()).isEqualTo(PostFixture.DEFAULT_CONTENT);
    }

    @Test
    @DisplayName("회원 정보와 함께 게시글을 조회할 수 있다")
    void findByIdWithMember() {
        Post post = postRepository.save(PostFixture.create(member));

        Post found = postRepository.findByIdWithMember(post.getId()).orElseThrow();

        assertThat(found.getId()).isEqualTo(post.getId());
        assertThat(found.getMember().getId()).isEqualTo(member.getId());
        assertThat(found.getMember().getNickname()).isEqualTo("tester");
    }

    @Test
    @DisplayName("삭제된 게시글은 findByIdWithMember로 조회되지 않는다")
    void findByIdWithMember_deletedPost() {
        Post post = postRepository.save(PostFixture.create(member));
        post.delete();
        postRepository.save(post);

        assertThat(postRepository.findByIdWithMember(post.getId())).isEmpty();
    }

    @Test
    @DisplayName("좋아요 수를 증가시킬 수 있다")
    void incrementLikeCount() {
        Post post = postRepository.save(PostFixture.create(member));
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
        Post post = postRepository.save(PostFixture.create(member));
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
        Post post = postRepository.save(PostFixture.create(member));
        Long postId = post.getId();

        postRepository.decrementLikeCount(postId);
        postRepository.flush();

        Post found = postRepository.findById(postId).orElseThrow();
        assertThat(found.getLikeCount()).isEqualTo(0L);
    }

    @Test
    @DisplayName("조회수를 증가시킬 수 있다")
    void incrementViewCount() {
        Post post = postRepository.save(PostFixture.create(member));
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
        Post post = postRepository.save(PostFixture.create(member));
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
        Post post = postRepository.save(PostFixture.create(member));
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
        Post post = postRepository.save(PostFixture.create(member));
        Long postId = post.getId();

        postRepository.decrementCommentCount(postId);
        postRepository.flush();

        Post found = postRepository.findById(postId).orElseThrow();
        assertThat(found.getCommentCount()).isEqualTo(0L);
    }

    @Test
    @DisplayName("새 필드들이 정상적으로 저장되고 조회된다")
    void saveAndFindWithNewFields() {
        Post post = PostFixture.create(member);
        post.updateSummary("게시글 요약");
        post.updateVisibility("private");
        post.markAsDraft();
        post.setCommentsAllowed(false);
        post.updateThumbnail("https://example.com/thumb.jpg");
        post.updateImageUrl("https://example.com/image.jpg");

        Post saved = postRepository.save(post);
        Post found = postRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getSummary()).isEqualTo("게시글 요약");
        assertThat(found.getVisibility()).isEqualTo("private");
        assertThat(found.getIsDraft()).isTrue();
        assertThat(found.getCommentsAllowed()).isFalse();
        assertThat(found.getThumbnail()).isEqualTo("https://example.com/thumb.jpg");
        assertThat(found.getImageUrl()).isEqualTo("https://example.com/image.jpg");
    }

    @Test
    @DisplayName("시리즈와 함께 게시글을 저장하고 조회할 수 있다")
    void saveAndFindWithSeries() {
        Series series = seriesRepository.save(Series.create(member, "시리즈명", "설명"));

        Post post = PostFixture.create(member);
        post.setSeries(series);

        Post saved = postRepository.save(post);
        Post found = postRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getSeries()).isNotNull();
        assertThat(found.getSeries().getId()).isEqualTo(series.getId());
        assertThat(found.getSeries().getName()).isEqualTo("시리즈명");
    }

    @Test
    @DisplayName("시리즈를 제거할 수 있다")
    void removeSeries() {
        Series series = seriesRepository.save(Series.create(member, "시리즈명", "설명"));

        Post post = PostFixture.create(member);
        post.setSeries(series);
        Post saved = postRepository.save(post);

        saved.removeSeries();
        postRepository.save(saved);
        postRepository.flush();

        Post found = postRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getSeries()).isNull();
    }

    @Test
    @DisplayName("기본값으로 생성된 필드들이 정상적으로 저장된다")
    void saveWithDefaultValues() {
        Post post = PostFixture.create(member);
        Post saved = postRepository.save(post);
        Post found = postRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getPostTags()).isEmpty();
        assertThat(found.getVisibility()).isEqualTo("public");
        assertThat(found.getIsDraft()).isFalse();
        assertThat(found.getCommentsAllowed()).isTrue();
        assertThat(found.getSummary()).isNull();
        assertThat(found.getSeries()).isNull();
        assertThat(found.getThumbnail()).isNull();
        assertThat(found.getImageUrl()).isNull();
    }

    @Test
    @DisplayName("필드 수정 후 재조회 시 변경된 값이 반영된다")
    void updateAndReload() {
        Post post = postRepository.save(PostFixture.create(member));

        post.updateSummary("새 요약");
        post.publish();

        postRepository.save(post);
        postRepository.flush();

        Post reloaded = postRepository.findById(post.getId()).orElseThrow();

        assertThat(reloaded.getSummary()).isEqualTo("새 요약");
        assertThat(reloaded.getIsDraft()).isFalse();
    }
}
