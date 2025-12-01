package com.devon.techblog.application.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "소셜 링크 정보")
public record SocialLinks(
        @Schema(description = "GitHub URL", example = "https://github.com/username")
        String github,

        @Schema(description = "개인 웹사이트 URL", example = "https://example.com")
        String website,

        @Schema(description = "LinkedIn URL", example = "https://linkedin.com/in/username")
        String linkedin,

        @Schema(description = "Notion URL", example = "https://notion.so/username")
        String notion
) {
    public static SocialLinks empty() {
        return new SocialLinks(null, null, null, null);
    }
}
