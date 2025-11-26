package com.devon.techblog.application.post.dto.request;

import static com.devon.techblog.common.validation.ValidationMessages.INVALID_IMAGE_URL;
import static com.devon.techblog.common.validation.ValidationMessages.REQUIRED_POST_CONTENT;
import static com.devon.techblog.common.validation.ValidationMessages.REQUIRED_POST_TITLE;
import static com.devon.techblog.common.validation.ValidationPatterns.URL_PATTERN;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "게시글 수정 요청 DTO")
public record PostUpdateRequest(
        @Schema(description = "수정할 게시글 제목", example = "This is an updated title.")
        @NotBlank(message = REQUIRED_POST_TITLE)
        String title,

        @Schema(description = "수정할 게시글 내용", example = "This is an updated content.")
        @NotBlank(message = REQUIRED_POST_CONTENT)
        String content,

        @Schema(description = "수정할 이미지 URL", example = "https://picsum.photos/300")
        @Pattern(regexp = URL_PATTERN, message = INVALID_IMAGE_URL)
        String image
) {}