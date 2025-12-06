package com.devon.techblog.domain.post.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.devon.techblog.config.annotation.UnitTest;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UnitTest
class MarkdownImageExtractorTest {

    @Test
    @DisplayName("마크다운 content에서 이미지 URL들을 추출한다")
    void extractImageUrls_success() {
        String content = """
                게시글 내용입니다.

                ![이미지1](https://example.com/image1.jpg)

                더 많은 내용...

                ![이미지2](https://example.com/image2.png)
                """;

        List<String> urls = MarkdownImageExtractor.extractImageUrls(content);

        assertThat(urls).hasSize(2);
        assertThat(urls).containsExactly(
                "https://example.com/image1.jpg",
                "https://example.com/image2.png"
        );
    }

    @Test
    @DisplayName("이미지가 없는 content는 빈 리스트를 반환한다")
    void extractImageUrls_noImages() {
        String content = "이미지가 없는 일반 텍스트입니다.";

        List<String> urls = MarkdownImageExtractor.extractImageUrls(content);

        assertThat(urls).isEmpty();
    }

    @Test
    @DisplayName("null content는 빈 리스트를 반환한다")
    void extractImageUrls_nullContent() {
        List<String> urls = MarkdownImageExtractor.extractImageUrls(null);

        assertThat(urls).isEmpty();
    }

    @Test
    @DisplayName("빈 content는 빈 리스트를 반환한다")
    void extractImageUrls_emptyContent() {
        List<String> urls = MarkdownImageExtractor.extractImageUrls("");

        assertThat(urls).isEmpty();
    }

    @Test
    @DisplayName("http 프로토콜 이미지도 추출한다")
    void extractImageUrls_httpProtocol() {
        String content = "![이미지](http://example.com/image.jpg)";

        List<String> urls = MarkdownImageExtractor.extractImageUrls(content);

        assertThat(urls).hasSize(1);
        assertThat(urls.get(0)).isEqualTo("http://example.com/image.jpg");
    }

    @Test
    @DisplayName("마크다운 링크는 추출하지 않는다")
    void extractImageUrls_ignoreLinks() {
        String content = """
                [링크](https://example.com/page)
                ![이미지](https://example.com/image.jpg)
                """;

        List<String> urls = MarkdownImageExtractor.extractImageUrls(content);

        assertThat(urls).hasSize(1);
        assertThat(urls.get(0)).isEqualTo("https://example.com/image.jpg");
    }

    @Test
    @DisplayName("복잡한 URL도 정확히 추출한다")
    void extractImageUrls_complexUrl() {
        String content = "![이미지](https://res.cloudinary.com/demo/image/upload/v1234567890/folder/image-name_abc123.jpg?quality=auto&format=webp)";

        List<String> urls = MarkdownImageExtractor.extractImageUrls(content);

        assertThat(urls).hasSize(1);
        assertThat(urls.get(0)).isEqualTo("https://res.cloudinary.com/demo/image/upload/v1234567890/folder/image-name_abc123.jpg?quality=auto&format=webp");
    }
}
