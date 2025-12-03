package com.devon.techblog.domain.file.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.devon.techblog.config.annotation.UnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UnitTest
class FileTest {

    @Test
    @DisplayName("create 시 기본값이 설정된다")
    void create_setsDefaults() {
        File file = File.create(
                FileType.IMAGE,
                "test.jpg",
                "uploads/test.jpg",
                "https://example.com/test.jpg",
                1024L,
                "image/jpeg"
        );

        assertThat(file.getFileType()).isEqualTo(FileType.IMAGE);
        assertThat(file.getOriginalName()).isEqualTo("test.jpg");
        assertThat(file.getStorageKey()).isEqualTo("uploads/test.jpg");
        assertThat(file.getUrl()).isEqualTo("https://example.com/test.jpg");
        assertThat(file.getSize()).isEqualTo(1024L);
        assertThat(file.getMimeType()).isEqualTo("image/jpeg");
        assertThat(file.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("create 시 필수값이 없으면 예외가 발생한다")
    void create_requiresMandatoryFields() {
        assertThatThrownBy(() -> File.create(null, "test.jpg", "key", "url", 1L, "mime"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("file type required");

        assertThatThrownBy(() -> File.create(FileType.IMAGE, "", "key", "url", 1L, "mime"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("original name required");

        assertThatThrownBy(() -> File.create(FileType.IMAGE, "test.jpg", "", "url", 1L, "mime"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("storage key required");

        assertThatThrownBy(() -> File.create(FileType.IMAGE, "test.jpg", "key", "", 1L, "mime"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("url required");

        assertThatThrownBy(() -> File.create(FileType.IMAGE, "test.jpg", "key", "url", null, "mime"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("size required");

        assertThatThrownBy(() -> File.create(FileType.IMAGE, "test.jpg", "key", "url", 1L, ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mime type required");
    }

    @Test
    @DisplayName("size는 0보다 커야 한다")
    void create_sizeGuard() {
        assertThatThrownBy(() -> File.create(FileType.IMAGE, "test.jpg", "key", "url", 0L, "mime"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("size must be positive");

        assertThatThrownBy(() -> File.create(FileType.IMAGE, "test.jpg", "key", "url", -1L, "mime"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("size must be positive");

        File file = File.create(FileType.IMAGE, "test.jpg", "key", "url", 1L, "mime");
        assertThat(file.getSize()).isEqualTo(1L);
    }

    @Test
    @DisplayName("originalName은 255자를 초과할 수 없다")
    void create_originalNameLengthGuard() {
        assertThatThrownBy(() -> File.create(
                FileType.IMAGE,
                "a".repeat(256),
                "key",
                "url",
                1L,
                "mime"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("original name too long");

        File file = File.create(FileType.IMAGE, "a".repeat(255), "key", "url", 1L, "mime");
        assertThat(file.getOriginalName()).hasSize(255);
    }

    @Test
    @DisplayName("storageKey는 500자를 초과할 수 없다")
    void create_storageKeyLengthGuard() {
        assertThatThrownBy(() -> File.create(
                FileType.IMAGE,
                "test.jpg",
                "a".repeat(501),
                "url",
                1L,
                "mime"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("storage key too long");

        File file = File.create(FileType.IMAGE, "test.jpg", "a".repeat(500), "url", 1L, "mime");
        assertThat(file.getStorageKey()).hasSize(500);
    }

    @Test
    @DisplayName("url은 500자를 초과할 수 없다")
    void create_urlLengthGuard() {
        assertThatThrownBy(() -> File.create(
                FileType.IMAGE,
                "test.jpg",
                "key",
                "a".repeat(501),
                1L,
                "mime"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("url too long");

        File file = File.create(FileType.IMAGE, "test.jpg", "key", "a".repeat(500), 1L, "mime");
        assertThat(file.getUrl()).hasSize(500);
    }

    @Test
    @DisplayName("mimeType은 100자를 초과할 수 없다")
    void create_mimeTypeLengthGuard() {
        assertThatThrownBy(() -> File.create(
                FileType.IMAGE,
                "test.jpg",
                "key",
                "url",
                1L,
                "a".repeat(101)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mime type too long");

        File file = File.create(FileType.IMAGE, "test.jpg", "key", "url", 1L, "a".repeat(100));
        assertThat(file.getMimeType()).hasSize(100);
    }

    @Test
    @DisplayName("삭제 및 복구가 정상 동작한다")
    void deleteAndRestore() {
        File file = File.create(
                FileType.IMAGE,
                "test.jpg",
                "key",
                "url",
                1L,
                "mime"
        );

        file.delete();
        assertThat(file.isDeleted()).isTrue();

        file.restore();
        assertThat(file.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("다양한 FileType으로 파일을 생성할 수 있다")
    void create_withVariousFileTypes() {
        File image = File.create(FileType.IMAGE, "img.jpg", "key", "url", 1L, "image/jpeg");
        assertThat(image.getFileType()).isEqualTo(FileType.IMAGE);

        File video = File.create(FileType.VIDEO, "vid.mp4", "key", "url", 1L, "video/mp4");
        assertThat(video.getFileType()).isEqualTo(FileType.VIDEO);

        File document = File.create(FileType.DOCUMENT, "doc.pdf", "key", "url", 1L, "application/pdf");
        assertThat(document.getFileType()).isEqualTo(FileType.DOCUMENT);
    }
}
