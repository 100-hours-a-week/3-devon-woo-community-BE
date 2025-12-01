package com.devon.techblog.application.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.devon.techblog.config.annotation.UnitTest;
import com.devon.techblog.domain.member.MemberFixture;
import com.devon.techblog.domain.member.entity.Member;
import com.devon.techblog.domain.post.PostFixture;
import com.devon.techblog.domain.post.entity.Post;
import com.devon.techblog.domain.post.entity.PostTag;
import com.devon.techblog.domain.post.entity.Tag;
import com.devon.techblog.domain.post.repository.TagRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@UnitTest
class TagServiceTest {

    @Mock
    private TagRepository tagRepository;

    @InjectMocks
    private TagService tagService;

    private Post post;

    @BeforeEach
    void setUp() {
        Member member = MemberFixture.createWithId(1L);
        post = PostFixture.createWithId(1L, member);
    }

    @Test
    @DisplayName("새로운 태그로 PostTag를 생성할 수 있다")
    void createPostTags_withNewTags() {
        List<String> tagNames = List.of("Java", "Spring");
        Tag javaTag = createTagWithId(1L, "java");
        Tag springTag = createTagWithId(2L, "spring");

        given(tagRepository.findByName("java")).willReturn(Optional.empty());
        given(tagRepository.findByName("spring")).willReturn(Optional.empty());
        given(tagRepository.save(any(Tag.class)))
                .willReturn(javaTag)
                .willReturn(springTag);

        List<PostTag> postTags = tagService.createPostTags(post, tagNames);

        assertThat(postTags).hasSize(2);
        verify(tagRepository, times(2)).save(any(Tag.class));
        verify(tagRepository, times(2)).incrementUsageCount(anyLong());
    }

    @Test
    @DisplayName("기존 태그로 PostTag를 생성할 수 있다")
    void createPostTags_withExistingTags() {
        List<String> tagNames = List.of("java");
        Tag existingTag = createTagWithId(1L, "java");

        given(tagRepository.findByName("java")).willReturn(Optional.of(existingTag));

        List<PostTag> postTags = tagService.createPostTags(post, tagNames);

        assertThat(postTags).hasSize(1);
        verify(tagRepository, times(0)).save(any(Tag.class));
        verify(tagRepository, times(1)).incrementUsageCount(1L);
    }

    @Test
    @DisplayName("태그명이 대문자여도 소문자로 정규화하여 처리한다")
    void createPostTags_normalizesTagNames() {
        List<String> tagNames = List.of("JAVA", "Java", "java");
        Tag javaTag = createTagWithId(1L, "java");

        given(tagRepository.findByName("java")).willReturn(Optional.of(javaTag));

        List<PostTag> postTags = tagService.createPostTags(post, tagNames);

        assertThat(postTags).hasSize(3);
        verify(tagRepository, times(3)).findByName("java");
    }

    @Test
    @DisplayName("빈 문자열이나 공백만 있는 태그는 무시한다")
    void createPostTags_ignoresEmptyTags() {
        List<String> tagNames = List.of("java", "", "  ", "spring");
        Tag javaTag = createTagWithId(1L, "java");
        Tag springTag = createTagWithId(2L, "spring");

        given(tagRepository.findByName("java")).willReturn(Optional.of(javaTag));
        given(tagRepository.findByName("spring")).willReturn(Optional.of(springTag));

        List<PostTag> postTags = tagService.createPostTags(post, tagNames);

        assertThat(postTags).hasSize(2);
        verify(tagRepository, times(1)).findByName("java");
        verify(tagRepository, times(1)).findByName("spring");
    }

    @Test
    @DisplayName("null 또는 빈 리스트를 전달하면 빈 PostTag 리스트를 반환한다")
    void createPostTags_handlesNullAndEmptyList() {
        List<PostTag> nullResult = tagService.createPostTags(post, null);
        List<PostTag> emptyResult = tagService.createPostTags(post, new ArrayList<>());

        assertThat(nullResult).isEmpty();
        assertThat(emptyResult).isEmpty();
    }

    @Test
    @DisplayName("Post의 태그를 업데이트할 수 있다")
    void updatePostTags_success() {
        Tag oldTag = createTagWithId(1L, "java");
        Tag newTag = createTagWithId(2L, "spring");
        PostTag oldPostTag = PostTag.of(post, oldTag);

        post.addPostTag(oldPostTag);

        given(tagRepository.findByName("spring")).willReturn(Optional.of(newTag));

        tagService.updatePostTags(post, List.of("spring"));

        verify(tagRepository, times(1)).decrementUsageCount(1L);
        verify(tagRepository, times(1)).incrementUsageCount(2L);
    }

    @Test
    @DisplayName("인기 태그 상위 N개를 조회할 수 있다")
    void getTopTags() {
        Tag tag1 = createTagWithId(1L, "java");
        Tag tag2 = createTagWithId(2L, "spring");
        List<Tag> topTags = List.of(tag1, tag2);

        given(tagRepository.findTopByUsageCount(10)).willReturn(topTags);

        List<Tag> result = tagService.getTopTags(10);

        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(tag1, tag2);
        verify(tagRepository, times(1)).findTopByUsageCount(10);
    }

    private Tag createTagWithId(Long id, String name) {
        Tag tag = Tag.create(name);
        try {
            java.lang.reflect.Field idField = Tag.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(tag, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return tag;
    }
}
