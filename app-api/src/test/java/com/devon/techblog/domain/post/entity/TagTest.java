package com.devon.techblog.domain.post.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.devon.techblog.config.annotation.UnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UnitTest
class TagTest {

    @Test
    @DisplayName("create 시 태그명이 소문자로 정규화되고 usageCount가 0으로 초기화된다")
    void create_normalizesNameAndInitializesUsageCount() {
        Tag tag = Tag.create("Java");

        assertThat(tag.getName()).isEqualTo("java");
        assertThat(tag.getUsageCount()).isEqualTo(0L);
    }

    @Test
    @DisplayName("create 시 태그명 앞뒤 공백이 제거된다")
    void create_trimsWhitespace() {
        Tag tag = Tag.create("  Spring Boot  ");

        assertThat(tag.getName()).isEqualTo("spring boot");
    }

    @Test
    @DisplayName("create 시 빈 태그명은 예외가 발생한다")
    void create_requiresNonEmptyName() {
        assertThatThrownBy(() -> Tag.create(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tag name required");

        assertThatThrownBy(() -> Tag.create("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tag name required");
    }

    @Test
    @DisplayName("create 시 태그명이 50자를 초과하면 예외가 발생한다")
    void create_tagNameLengthGuard() {
        String longTag = "a".repeat(51);

        assertThatThrownBy(() -> Tag.create(longTag))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tag name too long");
    }

    @Test
    @DisplayName("태그명이 정확히 50자일 때는 생성 가능하다")
    void create_allowsExactly50Characters() {
        String exactTag = "a".repeat(50);

        Tag tag = Tag.create(exactTag);

        assertThat(tag.getName()).hasSize(50);
    }
}
