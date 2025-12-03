package com.devon.techblog.domain.post.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkdownImageExtractor {

    private static final Pattern MARKDOWN_IMAGE_PATTERN = Pattern.compile("!\\[.*?\\]\\((https?://[^)]+)\\)");

    public static List<String> extractImageUrls(String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }

        List<String> urls = new ArrayList<>();
        Matcher matcher = MARKDOWN_IMAGE_PATTERN.matcher(content);

        while (matcher.find()) {
            urls.add(matcher.group(1));
        }

        return urls;
    }
}
