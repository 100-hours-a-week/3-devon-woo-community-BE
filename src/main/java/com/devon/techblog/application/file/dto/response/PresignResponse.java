package com.devon.techblog.application.file.dto.response;

public record PresignResponse(
        Long fileId,
        String storageKey,
        UploadSignature uploadSignature
) {
    public record UploadSignature(
            String apiKey,
            String cloudName,
            Long timestamp,
            String signature,
            String uploadPreset,
            String folder
    ) {
    }
}
