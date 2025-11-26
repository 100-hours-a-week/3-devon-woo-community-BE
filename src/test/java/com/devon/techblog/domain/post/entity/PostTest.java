package com.devon.techblog.domain.post.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.devon.techblog.config.annotation.UnitTest;
import com.devon.techblog.domain.member.entity.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UnitTest
class PostTest {

    @Test
    @DisplayName("create 시 기본 카운트와 삭제 상태가 설정된다")
    void create_setsDefaultCountsAndDeleteStatus() {
        Member member = Member.create("user@test.com", "password123", "tester");
        Post post = Post.create(member, "제목", "내용");

        assertThat(post.getViewsCount()).isEqualTo(0L);
        assertThat(post.getLikeCount()).isEqualTo(0L);
        assertThat(post.getCommentCount()).isEqualTo(0L);
        assertThat(post.getIsDeleted()).isFalse();
        assertThat(post.getMember()).isEqualTo(member);
        assertThat(post.getTitle()).isEqualTo("제목");
        assertThat(post.getContent()).isEqualTo("내용");
    }

    @Test
    @DisplayName("create 시 필수값이 없으면 예외가 발생한다")
    void create_requiresMandatoryFields() {
        Member member = Member.create("user@test.com", "password123", "tester");

        assertThatThrownBy(() -> Post.create(null, "title", "content"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> Post.create(member, "", "content"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> Post.create(member, "title", ""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("제목은 200자를 초과할 수 없다")
    void create_titleLengthGuard() {
        Member member = Member.create("user@test.com", "password123", "tester");

        assertThatThrownBy(() -> Post.create(member, "a".repeat(201), "content"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("title too long");

        Post post = Post.create(member, "a".repeat(200), "content");
        assertThat(post.getTitle()).hasSize(200);
    }

    @Test
    @DisplayName("게시글 수정 시 제목과 내용이 업데이트된다")
    void updatePost_updatesFields() {
        Member member = Member.create("user@test.com", "password123", "tester");
        Post post = Post.create(member, "원래제목", "원래내용");

        post.updatePost("새제목", "새내용");

        assertThat(post.getTitle()).isEqualTo("새제목");
        assertThat(post.getContent()).isEqualTo("새내용");
    }

    @Test
    @DisplayName("게시글 수정 시 제목은 200자를 초과할 수 없다")
    void updatePost_titleLengthGuard() {
        Member member = Member.create("user@test.com", "password123", "tester");
        Post post = Post.create(member, "제목", "내용");

        assertThatThrownBy(() -> post.updatePost("a".repeat(201), "내용"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("title too long");
    }

    @Test
    @DisplayName("게시글 수정 시 빈 값은 허용되지 않는다")
    void updatePost_requiresNonBlankValues() {
        Member member = Member.create("user@test.com", "password123", "tester");
        Post post = Post.create(member, "제목", "내용");

        assertThatThrownBy(() -> post.updatePost("", "내용"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> post.updatePost("제목", " "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("조회수 증가가 정상 동작한다")
    void incrementViews_works() {
        Member member = Member.create("user@test.com", "password123", "tester");
        Post post = Post.create(member, "제목", "내용");

        post.incrementViews();
        assertThat(post.getViewsCount()).isEqualTo(1L);

        post.incrementViews();
        assertThat(post.getViewsCount()).isEqualTo(2L);
    }

    @Test
    @DisplayName("좋아요 증가 및 감소가 정상 동작한다")
    void likesIncreaseAndDecrease() {
        Member member = Member.create("user@test.com", "password123", "tester");
        Post post = Post.create(member, "제목", "내용");

        post.incrementLikes();
        assertThat(post.getLikeCount()).isEqualTo(1L);

        post.incrementLikes();
        assertThat(post.getLikeCount()).isEqualTo(2L);

        post.decrementLikes();
        assertThat(post.getLikeCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("좋아요 개수는 0 미만으로 내려가지 않는다")
    void decrementLikes_doesNotGoBelowZero() {
        Member member = Member.create("user@test.com", "password123", "tester");
        Post post = Post.create(member, "제목", "내용");

        post.decrementLikes();
        assertThat(post.getLikeCount()).isEqualTo(0L);

        post.decrementLikes();
        assertThat(post.getLikeCount()).isEqualTo(0L);
    }

    @Test
    @DisplayName("삭제 및 복구가 정상 동작한다")
    void deleteAndRestore() {
        Member member = Member.create("user@test.com", "password123", "tester");
        Post post = Post.create(member, "제목", "내용");

        post.delete();
        assertThat(post.isDeleted()).isTrue();
        assertThat(post.getIsDeleted()).isTrue();

        post.restore();
        assertThat(post.isDeleted()).isFalse();
        assertThat(post.getIsDeleted()).isFalse();
    }
}
