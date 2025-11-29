package com.devon.techblog.domain.post.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.devon.techblog.config.annotation.UnitTest;
import com.devon.techblog.domain.member.entity.Member;
import java.util.Arrays;
import java.util.List;
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

    @Test
    @DisplayName("create 시 새 필드들이 기본값으로 초기화된다")
    void create_initializesNewFieldsWithDefaults() {
        Member member = Member.create("user@test.com", "password123", "tester");
        Post post = Post.create(member, "제목", "내용");

        assertThat(post.getTags()).isEmpty();
        assertThat(post.getVisibility()).isEqualTo("public");
        assertThat(post.getIsDraft()).isFalse();
        assertThat(post.getCommentsAllowed()).isTrue();
        assertThat(post.getSummary()).isNull();
        assertThat(post.getSeries()).isNull();
        assertThat(post.getThumbnail()).isNull();
        assertThat(post.getImageUrl()).isNull();
    }

    @Test
    @DisplayName("요약은 500자를 초과할 수 없다")
    void updateSummary_lengthGuard() {
        Member member = Member.create("user@test.com", "password123", "tester");
        Post post = Post.create(member, "제목", "내용");

        post.updateSummary("짧은 요약");
        assertThat(post.getSummary()).isEqualTo("짧은 요약");

        post.updateSummary(null);
        assertThat(post.getSummary()).isNull();

        assertThatThrownBy(() -> post.updateSummary("a".repeat(501)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("summary too long");
    }

    @Test
    @DisplayName("태그 목록을 전체 교체할 수 있다")
    void updateTags_replacesAllTags() {
        Member member = Member.create("user@test.com", "password123", "tester");
        Post post = Post.create(member, "제목", "내용");

        List<String> tags = Arrays.asList("Java", "Spring", "JPA");
        post.updateTags(tags);
        assertThat(post.getTags()).containsExactly("Java", "Spring", "JPA");

        post.updateTags(Arrays.asList("Kotlin"));
        assertThat(post.getTags()).containsExactly("Kotlin");

        post.updateTags(null);
        assertThat(post.getTags()).isEmpty();
    }

    @Test
    @DisplayName("태그를 개별적으로 추가할 수 있다")
    void addTag_addsIndividualTag() {
        Member member = Member.create("user@test.com", "password123", "tester");
        Post post = Post.create(member, "제목", "내용");

        post.addTag("Java");
        assertThat(post.getTags()).containsExactly("Java");

        post.addTag("Spring");
        assertThat(post.getTags()).containsExactly("Java", "Spring");

        post.addTag("Java");
        assertThat(post.getTags()).containsExactly("Java", "Spring");
    }

    @Test
    @DisplayName("태그는 50자를 초과할 수 없다")
    void addTag_lengthGuard() {
        Member member = Member.create("user@test.com", "password123", "tester");
        Post post = Post.create(member, "제목", "내용");

        assertThatThrownBy(() -> post.addTag("a".repeat(51)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tag too long");

        assertThatThrownBy(() -> post.addTag(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("태그를 개별적으로 제거할 수 있다")
    void removeTag_removesTag() {
        Member member = Member.create("user@test.com", "password123", "tester");
        Post post = Post.create(member, "제목", "내용");

        post.updateTags(Arrays.asList("Java", "Spring", "JPA"));
        post.removeTag("Spring");
        assertThat(post.getTags()).containsExactly("Java", "JPA");

        post.removeTag("NonExistent");
        assertThat(post.getTags()).containsExactly("Java", "JPA");
    }

    @Test
    @DisplayName("시리즈를 설정하고 제거할 수 있다")
    void setAndRemoveSeries() {
        Member member = Member.create("user@test.com", "password123", "tester");
        Post post = Post.create(member, "제목", "내용");
        Series series = Series.create(member, "시리즈명", "설명");

        post.setSeries(series);
        assertThat(post.getSeries()).isEqualTo(series);

        post.removeSeries();
        assertThat(post.getSeries()).isNull();
    }

    @Test
    @DisplayName("공개 범위를 변경할 수 있다")
    void updateVisibility_changesVisibility() {
        Member member = Member.create("user@test.com", "password123", "tester");
        Post post = Post.create(member, "제목", "내용");

        post.updateVisibility("private");
        assertThat(post.getVisibility()).isEqualTo("private");

        assertThatThrownBy(() -> post.updateVisibility("a".repeat(21)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("visibility too long");
    }

    @Test
    @DisplayName("임시 저장 및 발행 상태를 전환할 수 있다")
    void draftAndPublish() {
        Member member = Member.create("user@test.com", "password123", "tester");
        Post post = Post.create(member, "제목", "내용");

        post.markAsDraft();
        assertThat(post.getIsDraft()).isTrue();

        post.publish();
        assertThat(post.getIsDraft()).isFalse();
    }

    @Test
    @DisplayName("댓글 허용 여부를 설정할 수 있다")
    void setCommentsAllowed_setsFlag() {
        Member member = Member.create("user@test.com", "password123", "tester");
        Post post = Post.create(member, "제목", "내용");

        post.setCommentsAllowed(false);
        assertThat(post.getCommentsAllowed()).isFalse();

        post.setCommentsAllowed(true);
        assertThat(post.getCommentsAllowed()).isTrue();

        post.setCommentsAllowed(null);
        assertThat(post.getCommentsAllowed()).isTrue();
    }

    @Test
    @DisplayName("썸네일 URL을 설정할 수 있다")
    void updateThumbnail_setsUrl() {
        Member member = Member.create("user@test.com", "password123", "tester");
        Post post = Post.create(member, "제목", "내용");

        post.updateThumbnail("https://example.com/thumb.jpg");
        assertThat(post.getThumbnail()).isEqualTo("https://example.com/thumb.jpg");

        post.updateThumbnail(null);
        assertThat(post.getThumbnail()).isNull();

        assertThatThrownBy(() -> post.updateThumbnail("https://example.com/" + "a".repeat(481)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("thumbnail url too long");
    }

    @Test
    @DisplayName("이미지 URL을 설정할 수 있다")
    void updateImageUrl_setsUrl() {
        Member member = Member.create("user@test.com", "password123", "tester");
        Post post = Post.create(member, "제목", "내용");

        post.updateImageUrl("https://example.com/image.jpg");
        assertThat(post.getImageUrl()).isEqualTo("https://example.com/image.jpg");

        post.updateImageUrl(null);
        assertThat(post.getImageUrl()).isNull();

        assertThatThrownBy(() -> post.updateImageUrl("https://example.com/" + "a".repeat(481)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("image url too long");
    }
}
