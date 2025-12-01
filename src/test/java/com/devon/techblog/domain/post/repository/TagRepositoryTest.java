package com.devon.techblog.domain.post.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.devon.techblog.config.annotation.RepositoryJpaTest;
import com.devon.techblog.domain.post.entity.Tag;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@RepositoryJpaTest
class TagRepositoryTest {

    @Autowired
    private TagRepository tagRepository;

    @AfterEach
    void tearDown() {
        tagRepository.deleteAll();
    }

    @Test
    @DisplayName("태그를 저장하고 조회할 수 있다")
    void saveAndFind() {
        Tag tag = Tag.create("java");
        Tag saved = tagRepository.save(tag);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("java");
        assertThat(saved.getUsageCount()).isEqualTo(0L);
    }

    @Test
    @DisplayName("태그명으로 태그를 조회할 수 있다")
    void findByName() {
        Tag tag = Tag.create("spring");
        tagRepository.save(tag);

        Optional<Tag> found = tagRepository.findByName("spring");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("spring");
    }

    @Test
    @DisplayName("존재하지 않는 태그명으로 조회하면 빈 Optional을 반환한다")
    void findByName_notFound() {
        Optional<Tag> found = tagRepository.findByName("nonexistent");

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("여러 태그명으로 태그를 조회할 수 있다")
    void findByNameIn() {
        tagRepository.save(Tag.create("java"));
        tagRepository.save(Tag.create("spring"));
        tagRepository.save(Tag.create("jpa"));

        List<Tag> tags = tagRepository.findByNameIn(List.of("java", "spring"));

        assertThat(tags).hasSize(2);
        assertThat(tags).extracting(Tag::getName)
                .containsExactlyInAnyOrder("java", "spring");
    }

    @Test
    @DisplayName("usageCount를 증가시킬 수 있다")
    void incrementUsageCount() {
        Tag tag = tagRepository.save(Tag.create("kotlin"));

        int updated = tagRepository.incrementUsageCount(tag.getId());
        tagRepository.flush();
        Tag found = tagRepository.findById(tag.getId()).orElseThrow();

        assertThat(updated).isEqualTo(1);
        assertThat(found.getUsageCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("usageCount를 감소시킬 수 있다")
    void decrementUsageCount() {
        Tag tag = tagRepository.save(Tag.create("python"));
        tagRepository.incrementUsageCount(tag.getId());
        tagRepository.flush();

        int updated = tagRepository.decrementUsageCount(tag.getId());
        tagRepository.flush();
        Tag found = tagRepository.findById(tag.getId()).orElseThrow();

        assertThat(updated).isEqualTo(1);
        assertThat(found.getUsageCount()).isEqualTo(0L);
    }

    @Test
    @DisplayName("usageCount가 0일 때 감소시키면 음수가 되지 않는다")
    void decrementUsageCount_notBelowZero() {
        Tag tag = tagRepository.save(Tag.create("rust"));

        int updated = tagRepository.decrementUsageCount(tag.getId());
        tagRepository.flush();
        Tag found = tagRepository.findById(tag.getId()).orElseThrow();

        assertThat(updated).isEqualTo(0);
        assertThat(found.getUsageCount()).isEqualTo(0L);
    }

    @Test
    @DisplayName("usageCount 내림차순으로 상위 태그를 조회할 수 있다")
    void findTopByUsageCount() {
        Tag tag1 = tagRepository.save(Tag.create("java"));
        Tag tag2 = tagRepository.save(Tag.create("spring"));
        Tag tag3 = tagRepository.save(Tag.create("jpa"));

        tagRepository.incrementUsageCount(tag1.getId());
        tagRepository.incrementUsageCount(tag1.getId());
        tagRepository.incrementUsageCount(tag2.getId());
        tagRepository.incrementUsageCount(tag2.getId());
        tagRepository.incrementUsageCount(tag2.getId());
        tagRepository.incrementUsageCount(tag3.getId());
        tagRepository.flush();

        List<Tag> topTags = tagRepository.findTopByUsageCount(2);

        assertThat(topTags).hasSize(2);
        assertThat(topTags.get(0).getName()).isEqualTo("spring");
        assertThat(topTags.get(0).getUsageCount()).isEqualTo(3L);
        assertThat(topTags.get(1).getName()).isEqualTo("java");
        assertThat(topTags.get(1).getUsageCount()).isEqualTo(2L);
    }
}
