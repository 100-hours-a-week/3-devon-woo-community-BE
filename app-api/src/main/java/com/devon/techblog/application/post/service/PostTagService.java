package com.devon.techblog.application.post.service;

import com.devon.techblog.domain.post.entity.Post;
import com.devon.techblog.domain.post.entity.PostTag;
import com.devon.techblog.domain.post.entity.Tag;
import com.devon.techblog.domain.post.repository.TagRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostTagService {

    private final TagRepository tagRepository;

    /**
     * 게시글의 Tag들을 저장합니다 (Bulk Update)
     */
    @Transactional
    public List<PostTag> createPostTags(Post post, List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> normalizedNames = normalizeTagNames(tagNames);
        if (normalizedNames.isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, Tag> tags = getOrCreateTags(normalizedNames);

        List<Long> tagIds = normalizedNames.stream()
                .map(tags::get)
                .map(Tag::getId)
                .toList();

        if (!tagIds.isEmpty()) {
            tagRepository.bulkIncrementUsageCount(tagIds);
        }

        return normalizedNames.stream()
                .map(tags::get)
                .map(tag -> PostTag.create(post, tag))
                .collect(Collectors.toList());
    }

    /**
     * 게시글의 Tag들을 저장합니다 (Bulk Update)
     */
    @Transactional
    public void updatePostTags(Post post, List<String> newTagNames) {
        Set<String> oldTagNames = post.getPostTags().stream()
                .map(postTag -> postTag.getTag().getName())
                .collect(Collectors.toSet());

        List<String> normalizedNewNames = normalizeTagNames(newTagNames != null ? newTagNames : List.of());
        Set<String> newTagNameSet = new HashSet<>(normalizedNewNames);

        Set<String> tagsToRemove = new HashSet<>(oldTagNames);
        tagsToRemove.removeAll(newTagNameSet);

        Set<String> tagsToAdd = new HashSet<>(newTagNameSet);
        tagsToAdd.removeAll(oldTagNames);

        if (!tagsToRemove.isEmpty()) {
            List<Long> tagIdsToDecrement = post.getPostTags().stream()
                    .filter(postTag -> tagsToRemove.contains(postTag.getTag().getName()))
                    .map(postTag -> postTag.getTag().getId())
                    .toList();

            if (!tagIdsToDecrement.isEmpty()) {
                tagRepository.bulkDecrementUsageCount(tagIdsToDecrement);
            }

            post.getPostTags().removeIf(postTag -> tagsToRemove.contains(postTag.getTag().getName()));
        }

        if (!tagsToAdd.isEmpty()) {
            List<String> addNames = new ArrayList<>(tagsToAdd);
            Map<String, Tag> tags = getOrCreateTags(addNames);

            List<Long> tagIdsToIncrement = addNames.stream()
                    .map(tags::get)
                    .map(Tag::getId)
                    .toList();

            if (!tagIdsToIncrement.isEmpty()) {
                tagRepository.bulkIncrementUsageCount(tagIdsToIncrement);
            }

            addNames.forEach(name -> {
                Tag tag = tags.get(name);
                post.addPostTag(PostTag.create(post, tag));
            });
        }
    }

    /**
     * 가장 많이 조회된 Top 태그
     */
    @Transactional(readOnly = true)
    public List<Tag> getTopTags(int limit) {
        return tagRepository.findTopByUsageCount(limit);
    }

    private List<String> normalizeTagNames(List<String> tagNames) {
        return tagNames.stream()
                .map(name -> name.trim().toLowerCase())
                .filter(name -> !name.isEmpty())
                .distinct()
                .toList();
    }

    private Map<String, Tag> getOrCreateTags(List<String> normalizedNames) {
        Map<String, Tag> existingTags = tagRepository.findByNameIn(normalizedNames).stream()
                .collect(Collectors.toMap(Tag::getName, tag -> tag));

        List<Tag> newTags = normalizedNames.stream()
                .filter(name -> !existingTags.containsKey(name))
                .map(Tag::create)
                .toList();

        if (!newTags.isEmpty()) {
            List<Tag> savedNewTags = tagRepository.saveAll(newTags);
            savedNewTags.forEach(tag -> existingTags.put(tag.getName(), tag));
        }

        return existingTags;
    }
}
