package com.devon.techblog.application.file.dto.response;

import com.devon.techblog.infra.image.ImageSignature;

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

    public static PresignResponse from(Long fileId, String storageKey, ImageSignature signature) {
        return new PresignResponse(
                fileId,
                storageKey,
                new UploadSignature(
                        signature.apiKey(),
                        signature.cloudName(),
                        signature.timestamp(),
                        signature.signature(),
                        signature.uploadPreset(),
                        signature.folder()
                )
        );
    }
}
