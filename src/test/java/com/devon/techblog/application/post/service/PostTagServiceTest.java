package com.devon.techblog.application.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
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
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@UnitTest
class PostTagServiceTest {

    @Mock
    private TagRepository tagRepository;

    @InjectMocks
    private PostTagService postTagService;

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

        given(tagRepository.findByNameIn(anyList())).willReturn(Collections.emptyList());
        given(tagRepository.saveAll(anyList())).willReturn(List.of(javaTag, springTag));

        List<PostTag> postTags = postTagService.createPostTags(post, tagNames);

        assertThat(postTags).hasSize(2);
        verify(tagRepository, times(1)).saveAll(anyList());
        verify(tagRepository, times(1)).bulkIncrementUsageCount(anyList());
    }

    @Test
    @DisplayName("기존 태그로 PostTag를 생성할 수 있다")
    void createPostTags_withExistingTags() {
        List<String> tagNames = List.of("java");
        Tag existingTag = createTagWithId(1L, "java");

        given(tagRepository.findByNameIn(anyList())).willReturn(List.of(existingTag));

        List<PostTag> postTags = postTagService.createPostTags(post, tagNames);

        assertThat(postTags).hasSize(1);
        verify(tagRepository, times(0)).saveAll(anyList());
        verify(tagRepository, times(1)).bulkIncrementUsageCount(anyList());
    }

    @Test
    @DisplayName("태그명이 대문자여도 소문자로 정규화하여 처리한다")
    void createPostTags_normalizesTagNames() {
        List<String> tagNames = List.of("JAVA", "Java", "java");
        Tag javaTag = createTagWithId(1L, "java");

        given(tagRepository.findByNameIn(anyList())).willReturn(List.of(javaTag));

        List<PostTag> postTags = postTagService.createPostTags(post, tagNames);

        assertThat(postTags).hasSize(1);
        verify(tagRepository, times(1)).findByNameIn(anyList());
    }

    @Test
    @DisplayName("빈 문자열이나 공백만 있는 태그는 무시한다")
    void createPostTags_ignoresEmptyTags() {
        List<String> tagNames = List.of("java", "", "  ", "spring");
        Tag javaTag = createTagWithId(1L, "java");
        Tag springTag = createTagWithId(2L, "spring");

        given(tagRepository.findByNameIn(anyList())).willReturn(List.of(javaTag, springTag));

        List<PostTag> postTags = postTagService.createPostTags(post, tagNames);

        assertThat(postTags).hasSize(2);
        verify(tagRepository, times(1)).findByNameIn(anyList());
    }

    @Test
    @DisplayName("null 또는 빈 리스트를 전달하면 빈 PostTag 리스트를 반환한다")
    void createPostTags_handlesNullAndEmptyList() {
        List<PostTag> nullResult = postTagService.createPostTags(post, null);
        List<PostTag> emptyResult = postTagService.createPostTags(post, new ArrayList<>());

        assertThat(nullResult).isEmpty();
        assertThat(emptyResult).isEmpty();
    }

    @Test
    @DisplayName("Post의 태그를 업데이트할 수 있다")
    void updatePostTags_success() {
        Tag oldTag = createTagWithId(1L, "java");
        Tag newTag = createTagWithId(2L, "spring");
        PostTag oldPostTag = PostTag.create(post, oldTag);

        post.addPostTag(oldPostTag);

        given(tagRepository.findByNameIn(anyList())).willReturn(List.of(newTag));

        postTagService.updatePostTags(post, List.of("spring"));

        verify(tagRepository, times(1)).bulkDecrementUsageCount(anyList());
        verify(tagRepository, times(1)).bulkIncrementUsageCount(anyList());
    }

    @Test
    @DisplayName("동일한 태그로 업데이트하면 변경사항이 없다")
    void updatePostTags_noChange() {
        Tag javaTag = createTagWithId(1L, "java");
        PostTag postTag = PostTag.create(post, javaTag);

        post.addPostTag(postTag);

        postTagService.updatePostTags(post, List.of("java"));

        verify(tagRepository, times(0)).bulkDecrementUsageCount(anyList());
        verify(tagRepository, times(0)).bulkIncrementUsageCount(anyList());
        assertThat(post.getPostTags()).hasSize(1);
    }

    @Test
    @DisplayName("일부 태그만 변경할 수 있다")
    void updatePostTags_partialChange() {
        Tag javaTag = createTagWithId(1L, "java");
        Tag springTag = createTagWithId(2L, "spring");
        Tag kotlinTag = createTagWithId(3L, "kotlin");

        post.addPostTag(PostTag.create(post, javaTag));
        post.addPostTag(PostTag.create(post, springTag));

        given(tagRepository.findByNameIn(anyList())).willReturn(List.of(springTag, kotlinTag));

        postTagService.updatePostTags(post, List.of("spring", "kotlin"));

        verify(tagRepository, times(1)).bulkDecrementUsageCount(anyList());
        verify(tagRepository, times(1)).bulkIncrementUsageCount(anyList());
    }

    @Test
    @DisplayName("인기 태그 상위 N개를 조회할 수 있다")
    void getTopTags() {
        Tag tag1 = createTagWithId(1L, "java");
        Tag tag2 = createTagWithId(2L, "spring");
        List<Tag> topTags = List.of(tag1, tag2);

        given(tagRepository.findTopByUsageCount(10)).willReturn(topTags);

        List<Tag> result = postTagService.getTopTags(10);

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
