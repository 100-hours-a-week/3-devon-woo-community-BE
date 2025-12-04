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
    
    private record TagDiff(Set<String> tagsToAdd, Set<String> tagsToRemove) {}

    /**
     * 게시글의 태그를 생성하고 사용 횟수를 갱신합니다.
     */
    @Transactional
    public List<PostTag> createPostTags(Post post, List<String> tagNames) {
        List<String> normalizedNames = normalizeTagNames(tagNames);
        if (normalizedNames.isEmpty()) {
            return new ArrayList<>();
        }

        return createPostTagsForNormalizedNames(post, normalizedNames);
    }

    /**
     * 게시글의 태그를 변경하고 사용 횟수를 일괄 갱신합니다.
     */
    @Transactional
    public void updatePostTags(Post post, List<String> newTagNames) {
        TagDiff tagDiff = calculateTagDiff(post, newTagNames);

        handleRemovedTags(post, tagDiff.tagsToRemove());
        handleAddedTags(post, tagDiff.tagsToAdd());
    }

    /**
     * 가장 많이 조회된 Top 태그
     */
    @Transactional(readOnly = true)
    public List<Tag> getTopTags(int limit) {
        return tagRepository.findTopByUsageCount(limit);
    }

    /// =========== Private 메서드 ============ ///

    /// 게시글에 이미 연결된 태그 이름과 새 태그 이름을 비교하여 변경 사항을 계산합니다
    private TagDiff calculateTagDiff(Post post, List<String> newTagNames) {
        Set<String> oldTagNames = post.getPostTags().stream()
                .map(postTag -> postTag.getTag().getName())
                .collect(Collectors.toSet());

        List<String> normalizedNewNames = normalizeTagNames(newTagNames);
        Set<String> newTagNameSet = new HashSet<>(normalizedNewNames);

        Set<String> tagsToRemove = new HashSet<>(oldTagNames);
        tagsToRemove.removeAll(newTagNameSet);

        Set<String> tagsToAdd = new HashSet<>(newTagNameSet);
        tagsToAdd.removeAll(oldTagNames);

        return new TagDiff(tagsToAdd, tagsToRemove);
    }

    /// 제거 대상 태그를 Post에서 삭제하고 사용 횟수를 감소시킵니다.
    private void handleRemovedTags(Post post, Set<String> tagsToRemove) {
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

    }

    /// 추가 대상 태그를 생성하거나 조회하고 Post에 연결한 뒤 사용 횟수를 증가시킵니다.
    private void handleAddedTags(Post post, Set<String> tagsToAdd) {
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

    /// 태그 이름 리스트를 정규화하여 공백 제거, 소문자 변환 및 중복 제거를 수행합니다.
    private List<String> normalizeTagNames(List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return List.of();
        }

        return tagNames.stream()
                .map(name -> name.trim().toLowerCase())
                .filter(name -> !name.isEmpty())
                .distinct()
                .toList();
    }

    /// 정규화된 태그 이름을 기준으로 기존 태그를 조회하고, 없으면 새 태그를 생성합니다.
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

    /// 정규화된 태그 이름 목록을 기반으로 PostTag를 생성하고 사용 횟수를 갱신합니다.
    private List<PostTag> createPostTagsForNormalizedNames(Post post, List<String> normalizedNames) {
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
}
