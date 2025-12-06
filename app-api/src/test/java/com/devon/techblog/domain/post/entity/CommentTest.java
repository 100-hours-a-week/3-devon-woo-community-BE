package com.devon.techblog.domain.post.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.devon.techblog.config.annotation.UnitTest;
import com.devon.techblog.domain.member.entity.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UnitTest
class CommentTest {

    @Test
    @DisplayName("create 시 기본 삭제 상태가 설정된다")
    void create_setsDefaultDeleteStatus() {
        Member member = Member.create("user@test.com", "password123", "tester");
        Post post = Post.create(member, "제목", "내용");
        Comment comment = Comment.create(member, post, "댓글내용");

        assertThat(comment.getIsDeleted()).isFalse();
        assertThat(comment.getMember()).isEqualTo(member);
        assertThat(comment.getPost()).isEqualTo(post);
        assertThat(comment.getContent()).isEqualTo("댓글내용");
    }

    @Test
    @DisplayName("create 시 필수값이 없으면 예외가 발생한다")
    void create_requiresMandatoryFields() {
        Member member = Member.create("user@test.com", "password123", "tester");
        Post post = Post.create(member, "제목", "내용");

        assertThatThrownBy(() -> Comment.create(null, post, "content"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> Comment.create(member, null, "content"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> Comment.create(member, post, ""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("댓글 내용 수정이 정상 동작한다")
    void updateContent_works() {
        Member member = Member.create("user@test.com", "password123", "tester");
        Post post = Post.create(member, "제목", "내용");
        Comment comment = Comment.create(member, post, "원래댓글");

        comment.updateContent("수정된댓글");

        assertThat(comment.getContent()).isEqualTo("수정된댓글");
    }

    @Test
    @DisplayName("댓글 내용 수정 시 빈 값은 허용되지 않는다")
    void updateContent_requiresNonBlankValue() {
        Member member = Member.create("user@test.com", "password123", "tester");
        Post post = Post.create(member, "제목", "내용");
        Comment comment = Comment.create(member, post, "댓글");

        assertThatThrownBy(() -> comment.updateContent(""))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> comment.updateContent(" "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("삭제 및 복구가 정상 동작한다")
    void deleteAndRestore() {
        Member member = Member.create("user@test.com", "password123", "tester");
        Post post = Post.create(member, "제목", "내용");
        Comment comment = Comment.create(member, post, "댓글");

        comment.delete();
        assertThat(comment.isDeleted()).isTrue();
        assertThat(comment.getIsDeleted()).isTrue();

        comment.restore();
        assertThat(comment.isDeleted()).isFalse();
        assertThat(comment.getIsDeleted()).isFalse();
    }
}
