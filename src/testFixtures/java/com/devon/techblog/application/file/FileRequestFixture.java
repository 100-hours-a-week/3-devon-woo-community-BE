package com.devon.techblog.application.file;

import com.devon.techblog.application.file.dto.request.FileCreateRequest;
import com.devon.techblog.domain.file.FileFixture;
import com.devon.techblog.domain.file.entity.FileType;

public final class FileRequestFixture {

    private FileRequestFixture() {}

    public static FileCreateRequest createRequest() {
        return new FileCreateRequest(
                FileFixture.DEFAULT_FILE_TYPE,
                FileFixture.DEFAULT_ORIGINAL_NAME,
                FileFixture.DEFAULT_STORAGE_KEY,
                FileFixture.DEFAULT_URL,
                FileFixture.DEFAULT_SIZE,
                FileFixture.DEFAULT_MIME_TYPE
        );
    }

    public static FileCreateRequest createRequest(
            FileType fileType,
            String originalName,
            String storageKey,
            String url,
            Long size,
            String mimeType
    ) {
        return new FileCreateRequest(fileType, originalName, storageKey, url, size, mimeType);
    }

    public static FileCreateRequest createImageRequest() {
        return new FileCreateRequest(
                FileType.IMAGE,
                FileFixture.DEFAULT_ORIGINAL_NAME,
                FileFixture.DEFAULT_STORAGE_KEY,
                FileFixture.DEFAULT_URL,
                FileFixture.DEFAULT_SIZE,
                FileFixture.DEFAULT_MIME_TYPE
        );
    }

    public static FileCreateRequest createVideoRequest() {
        return new FileCreateRequest(
                FileType.VIDEO,
                FileFixture.VIDEO_ORIGINAL_NAME,
                FileFixture.VIDEO_STORAGE_KEY,
                FileFixture.VIDEO_URL,
                2048L,
                FileFixture.VIDEO_MIME_TYPE
        );
    }
}
