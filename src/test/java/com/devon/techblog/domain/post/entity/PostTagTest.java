package com.devon.techblog.domain.post.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.devon.techblog.config.annotation.UnitTest;
import com.devon.techblog.domain.post.PostFixture;
import com.devon.techblog.domain.post.TagFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UnitTest
class PostTagTest {

    private Post post;
    private Tag tag;

    @BeforeEach
    void setUp() {
        post = PostFixture.createWithId(1L);
        tag = TagFixture.createWitId(1L);
    }

    @Test
    @DisplayName("of 메서드로 PostTag를 생성할 수 있다")
    void create_createsPostTag() {
        PostTag postTag = PostTag.create(post, tag);

        assertThat(postTag.getPost()).isEqualTo(post);
        assertThat(postTag.getTag()).isEqualTo(tag);
        assertThat(postTag.getId()).isNotNull();
        assertThat(postTag.getId().getPostId()).isEqualTo(post.getId());
        assertThat(postTag.getId().getTagId()).isEqualTo(tag.getId());
    }

    @Test
    @DisplayName("of 메서드 호출 시 post가 null이면 예외가 발생한다")
    void create_requiresNonNullPost() {
        assertThatThrownBy(() -> PostTag.create(null, tag))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("post required");
    }

    @Test
    @DisplayName("of 메서드 호출 시 tag가 null이면 예외가 발생한다")
    void create_requiresNonNullTag() {
        assertThatThrownBy(() -> PostTag.create(post, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tag required");
    }
}
