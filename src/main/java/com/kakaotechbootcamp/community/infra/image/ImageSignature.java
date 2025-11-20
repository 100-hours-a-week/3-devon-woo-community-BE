package com.kakaotechbootcamp.community.infra.image;

public record ImageSignature(
        String apiKey,
        String cloudName,
        Long timestamp,
        String signature,
        String uploadPreset,
        String folder
) {
}