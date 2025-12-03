package com.devon.techblog.domain.file.util;

import com.devon.techblog.config.annotation.UnitTest;
import com.devon.techblog.domain.file.entity.FileType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@UnitTest
class FileStorageKeyGeneratorTest {

    @Test
    @DisplayName("파일 타입과 원본 파일명으로 storageKey를 생성한다")
    void generate_success() {
        String storageKey = FileStorageKeyGenerator.generate(FileType.IMAGE, "test.jpg");

        assertThat(storageKey).startsWith("images/");
        assertThat(storageKey).endsWith(".jpg");
        assertThat(storageKey).contains("-");
    }

    @Test
    @DisplayName("비디오 타입의 storageKey를 생성한다")
    void generate_video() {
        String storageKey = FileStorageKeyGenerator.generate(FileType.VIDEO, "demo.mp4");

        assertThat(storageKey).startsWith("videos/");
        assertThat(storageKey).endsWith(".mp4");
    }

    @Test
    @DisplayName("문서 타입의 storageKey를 생성한다")
    void generate_document() {
        String storageKey = FileStorageKeyGenerator.generate(FileType.DOCUMENT, "report.pdf");

        assertThat(storageKey).startsWith("documents/");
        assertThat(storageKey).endsWith(".pdf");
    }

    @Test
    @DisplayName("확장자가 없는 파일명도 처리한다")
    void generate_noExtension() {
        String storageKey = FileStorageKeyGenerator.generate(FileType.IMAGE, "readme");

        assertThat(storageKey).startsWith("images/");
        assertThat(storageKey).doesNotContain(".");
    }

    @Test
    @DisplayName("동일한 파일명으로 여러 번 생성해도 다른 storageKey를 반환한다")
    void generate_uniqueness() {
        String key1 = FileStorageKeyGenerator.generate(FileType.IMAGE, "test.jpg");
        String key2 = FileStorageKeyGenerator.generate(FileType.IMAGE, "test.jpg");

        assertThat(key1).isNotEqualTo(key2);
    }

    @Test
    @DisplayName("파일명에서 확장자를 추출한다")
    void extractExtension_success() {
        assertThat(FileStorageKeyGenerator.extractExtension("test.jpg")).isEqualTo(".jpg");
        assertThat(FileStorageKeyGenerator.extractExtension("image.png")).isEqualTo(".png");
        assertThat(FileStorageKeyGenerator.extractExtension("document.pdf")).isEqualTo(".pdf");
    }

    @Test
    @DisplayName("확장자가 없는 파일명은 빈 문자열을 반환한다")
    void extractExtension_noExtension() {
        assertThat(FileStorageKeyGenerator.extractExtension("readme")).isEmpty();
    }

    @Test
    @DisplayName("점으로 시작하는 파일명은 빈 문자열을 반환한다")
    void extractExtension_startsWithDot() {
        assertThat(FileStorageKeyGenerator.extractExtension(".gitignore")).isEmpty();
    }

    @Test
    @DisplayName("점으로 끝나는 파일명은 빈 문자열을 반환한다")
    void extractExtension_endsWithDot() {
        assertThat(FileStorageKeyGenerator.extractExtension("test.")).isEmpty();
    }

    @Test
    @DisplayName("여러 개의 점이 있는 파일명은 마지막 확장자를 추출한다")
    void extractExtension_multipleDots() {
        assertThat(FileStorageKeyGenerator.extractExtension("archive.tar.gz")).isEqualTo(".gz");
        assertThat(FileStorageKeyGenerator.extractExtension("my.file.name.txt")).isEqualTo(".txt");
    }
}
