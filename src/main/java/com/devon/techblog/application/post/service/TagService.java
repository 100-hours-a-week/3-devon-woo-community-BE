package com.devon.techblog.application.post.service;

import com.devon.techblog.domain.post.entity.Post;
import com.devon.techblog.domain.post.entity.PostTag;
import com.devon.techblog.domain.post.entity.Tag;
import com.devon.techblog.domain.post.repository.TagRepository;
import java.util.ArrayList;
import java.util.List;
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

        List<PostTag> postTags = new ArrayList<>();

        for (String tagName : tagNames) {
            String normalizedName = tagName.trim().toLowerCase();
            if (normalizedName.isEmpty()) {
                continue;
            }

            Tag tag = tagRepository.findByName(normalizedName)
                    .orElseGet(() -> {
                        Tag newTag = Tag.create(normalizedName);
                        return tagRepository.save(newTag);
                    });

            tagRepository.incrementUsageCount(tag.getId());

            PostTag postTag = PostTag.create(post, tag);
            postTags.add(postTag);
        }

        return postTags;
    }

    @Transactional
    public void updatePostTags(Post post, List<String> newTagNames) {
        List<PostTag> oldPostTags = new ArrayList<>(post.getPostTags());

        for (PostTag postTag : oldPostTags) {
            tagRepository.decrementUsageCount(postTag.getTag().getId());
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
