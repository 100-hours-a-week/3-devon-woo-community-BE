package com.devon.techblog.domain.post.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.devon.techblog.config.annotation.UnitTest;
import com.devon.techblog.domain.member.entity.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UnitTest
class SeriesTest {

    @Test
    @DisplayName("create 시 기본값이 설정된다")
    void create_setsDefaults() {
        Member member = Member.create("user@test.com", "password123", "tester");
        Series series = Series.create(member, "시리즈명", "시리즈 설명");

        assertThat(series.getMember()).isEqualTo(member);
        assertThat(series.getName()).isEqualTo("시리즈명");
        assertThat(series.getDescription()).isEqualTo("시리즈 설명");
        assertThat(series.getIsDeleted()).isFalse();
        assertThat(series.getThumbnail()).isNull();
    }

    @Test
    @DisplayName("create 시 필수값이 없으면 예외가 발생한다")
    void create_requiresMandatoryFields() {
        Member member = Member.create("user@test.com", "password123", "tester");

        assertThatThrownBy(() -> Series.create(null, "name", "desc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("member required");

        assertThatThrownBy(() -> Series.create(member, "", "desc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name required");

        assertThatThrownBy(() -> Series.create(member, null, "desc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name required");
    }

    @Test
    @DisplayName("시리즈명은 100자를 초과할 수 없다")
    void create_nameLengthGuard() {
        Member member = Member.create("user@test.com", "password123", "tester");

        assertThatThrownBy(() -> Series.create(member, "a".repeat(101), "desc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name too long");

        Series series = Series.create(member, "a".repeat(100), "desc");
        assertThat(series.getName()).hasSize(100);
    }

    @Test
    @DisplayName("시리즈 정보를 수정할 수 있다")
    void updateSeries_updatesFields() {
        Member member = Member.create("user@test.com", "password123", "tester");
        Series series = Series.create(member, "원래명", "원래설명");

        series.updateSeries("새이름", "새설명", "https://example.com/thumb.jpg");

        assertThat(series.getName()).isEqualTo("새이름");
        assertThat(series.getDescription()).isEqualTo("새설명");
        assertThat(series.getThumbnail()).isEqualTo("https://example.com/thumb.jpg");
    }

    @Test
    @DisplayName("시리즈 수정 시 이름은 100자를 초과할 수 없다")
    void updateSeries_nameLengthGuard() {
        Member member = Member.create("user@test.com", "password123", "tester");
        Series series = Series.create(member, "이름", "설명");

        assertThatThrownBy(() -> series.updateSeries("a".repeat(101), "설명", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name too long");
    }

    @Test
    @DisplayName("시리즈 수정 시 빈 이름은 허용되지 않는다")
    void updateSeries_requiresNonBlankName() {
        Member member = Member.create("user@test.com", "password123", "tester");
        Series series = Series.create(member, "이름", "설명");

        assertThatThrownBy(() -> series.updateSeries("", "설명", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name required");

        assertThatThrownBy(() -> series.updateSeries(" ", "설명", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name required");
    }

    @Test
    @DisplayName("썸네일 URL은 500자를 초과할 수 없다")
    void updateSeries_thumbnailLengthGuard() {
        Member member = Member.create("user@test.com", "password123", "tester");
        Series series = Series.create(member, "이름", "설명");

        assertThatThrownBy(() -> series.updateSeries("이름", "설명", "https://example.com/" + "a".repeat(482)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("thumbnail url too long");
    }

    @Test
    @DisplayName("시리즈 수정 시 null 값은 무시된다")
    void updateSeries_ignoresNullValues() {
        Member member = Member.create("user@test.com", "password123", "tester");
        Series series = Series.create(member, "원래명", "원래설명");

        series.updateSeries(null, null, null);

        assertThat(series.getName()).isEqualTo("원래명");
        assertThat(series.getDescription()).isEqualTo("원래설명");
        assertThat(series.getThumbnail()).isNull();
    }

    @Test
    @DisplayName("삭제 및 복구가 정상 동작한다")
    void deleteAndRestore() {
        Member member = Member.create("user@test.com", "password123", "tester");
        Series series = Series.create(member, "이름", "설명");

        series.delete();
        assertThat(series.isDeleted()).isTrue();
        assertThat(series.getIsDeleted()).isTrue();

        series.restore();
        assertThat(series.isDeleted()).isFalse();
        assertThat(series.getIsDeleted()).isFalse();
    }
}
