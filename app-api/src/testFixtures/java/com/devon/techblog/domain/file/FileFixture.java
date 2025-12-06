package com.devon.techblog.domain.file;

import com.devon.techblog.domain.file.entity.File;
import com.devon.techblog.domain.file.entity.FileType;
import org.springframework.test.util.ReflectionTestUtils;

public final class FileFixture {

    public static final String DEFAULT_ORIGINAL_NAME = "test-image.jpg";
    public static final String DEFAULT_STORAGE_KEY = "uploads/2024/01/test-image-uuid.jpg";
    public static final String DEFAULT_URL = "https://example.com/uploads/2024/01/test-image-uuid.jpg";
    public static final Long DEFAULT_SIZE = 1024L;
    public static final String DEFAULT_MIME_TYPE = "image/jpeg";
    public static final FileType DEFAULT_FILE_TYPE = FileType.IMAGE;

    public static final String VIDEO_ORIGINAL_NAME = "test-video.mp4";
    public static final String VIDEO_STORAGE_KEY = "uploads/2024/01/test-video-uuid.mp4";
    public static final String VIDEO_URL = "https://example.com/uploads/2024/01/test-video-uuid.mp4";
    public static final String VIDEO_MIME_TYPE = "video/mp4";

    public static final String DOCUMENT_ORIGINAL_NAME = "test-doc.pdf";
    public static final String DOCUMENT_STORAGE_KEY = "uploads/2024/01/test-doc-uuid.pdf";
    public static final String DOCUMENT_URL = "https://example.com/uploads/2024/01/test-doc-uuid.pdf";
    public static final String DOCUMENT_MIME_TYPE = "application/pdf";

    private FileFixture() {}

    public static File create() {
        return File.create(
                DEFAULT_FILE_TYPE,
                DEFAULT_ORIGINAL_NAME,
                DEFAULT_STORAGE_KEY,
                DEFAULT_URL,
                DEFAULT_SIZE,
                DEFAULT_MIME_TYPE
        );
    }

    public static File create(
            FileType fileType,
            String originalName,
            String storageKey,
            String url,
            Long size,
            String mimeType
    ) {
        return File.create(fileType, originalName, storageKey, url, size, mimeType);
    }

    public static File createImage() {
        return create(
                FileType.IMAGE,
                DEFAULT_ORIGINAL_NAME,
                DEFAULT_STORAGE_KEY,
                DEFAULT_URL,
                DEFAULT_SIZE,
                DEFAULT_MIME_TYPE
        );
    }

    public static File createVideo() {
        return create(
                FileType.VIDEO,
                VIDEO_ORIGINAL_NAME,
                VIDEO_STORAGE_KEY,
                VIDEO_URL,
                2048L,
                VIDEO_MIME_TYPE
        );
    }

    public static File createDocument() {
        return create(
                FileType.DOCUMENT,
                DOCUMENT_ORIGINAL_NAME,
                DOCUMENT_STORAGE_KEY,
                DOCUMENT_URL,
                512L,
                DOCUMENT_MIME_TYPE
        );
    }

    public static File createWithId(Long id) {
        File file = create();
        ReflectionTestUtils.setField(file, "id", id);
        return file;
    }

    public static File createWithId(
            Long id,
            FileType fileType,
            String originalName,
            String storageKey,
            String url,
            Long size,
            String mimeType
    ) {
        File file = create(fileType, originalName, storageKey, url, size, mimeType);
        ReflectionTestUtils.setField(file, "id", id);
        return file;
    }
}
