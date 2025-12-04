package com.devon.techblog.application.file.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CompleteUploadRequest(
        @NotBlank(message = "url is required")
        String url,

        @NotNull(message = "size is required")
        @Positive(message = "size must be positive")
        Long size
) {
}
