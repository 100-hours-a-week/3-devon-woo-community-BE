package com.devon.techblog.application.file.dto.request;

import com.devon.techblog.domain.file.entity.FileType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record FileCreateRequest(
        @NotNull(message = "fileType is required")
        FileType fileType,

        @NotBlank(message = "originalName is required")
        String originalName,

        @NotBlank(message = "storageKey is required")
        String storageKey,

        @NotBlank(message = "url is required")
        String url,

        @NotNull(message = "size is required")
        @Positive(message = "size must be positive")
        Long size,

        @NotBlank(message = "mimeType is required")
        String mimeType
) {
}
