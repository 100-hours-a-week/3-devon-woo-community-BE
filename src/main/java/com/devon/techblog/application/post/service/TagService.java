package com.devon.techblog.application.post.service;

import com.devon.techblog.domain.post.entity.Post;
import com.devon.techblog.domain.post.entity.PostTag;
import com.devon.techblog.domain.post.entity.Tag;
import com.devon.techblog.domain.post.repository.TagRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;

    @Transactional
    public List<PostTag> createPostTags(Post post, List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> normalizedNames = tagNames.stream()
                .map(name -> name.trim().toLowerCase())
                .filter(name -> !name.isEmpty())
                .distinct()
                .toList();

        if (normalizedNames.isEmpty()) {
            return new ArrayList<>();
        }

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

        List<Long> tagIds = normalizedNames.stream()
                .map(existingTags::get)
                .map(Tag::getId)
                .toList();

        if (!tagIds.isEmpty()) {
            tagRepository.bulkIncrementUsageCount(tagIds);
        }

        return normalizedNames.stream()
                .map(existingTags::get)
                .map(tag -> PostTag.create(post, tag))
                .collect(Collectors.toList());
    }

    @Transactional
    public void updatePostTags(Post post, List<String> newTagNames) {
        List<PostTag> oldPostTags = new ArrayList<>(post.getPostTags());

        List<Long> oldTagIds = oldPostTags.stream()
                .map(postTag -> postTag.getTag().getId())
                .toList();

        if (!oldTagIds.isEmpty()) {
            tagRepository.bulkDecrementUsageCount(oldTagIds);
        }

        post.clearPostTags();

        List<PostTag> newPostTags = createPostTags(post, newTagNames);
        for (PostTag postTag : newPostTags) {
            post.addPostTag(postTag);
        }
    }

    @Transactional(readOnly = true)
    public List<Tag> getTopTags(int limit) {
        return tagRepository.findTopByUsageCount(limit);
    }
}
