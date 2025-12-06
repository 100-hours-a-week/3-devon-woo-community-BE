package com.devon.techblog.domain.post.dto;

import java.util.List;

public record PostSearchCondition(
        String keyword,
        Long memberId,
        List<String> tags
) {

    public PostSearchCondition {
        keyword = normalizeKeyword(keyword);
        tags = normalizeTags(tags);
    }

    public static PostSearchCondition empty() {
        return new PostSearchCondition(null, null, null);
    }

    public static PostSearchCondition forTags(List<String> tags) {
        return new PostSearchCondition(null, null, tags);
    }

    private static String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }

        String trimmed = keyword.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static List<String> normalizeTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return null;
        }

        return List.copyOf(tags);
    }
}
