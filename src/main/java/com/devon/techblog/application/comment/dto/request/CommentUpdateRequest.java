package com.devon.techblog.application.comment.dto.request;

import static com.devon.techblog.common.validation.ValidationMessages.REQUIRED_COMMENT_CONTENT;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "댓글 수정 요청 DTO")
public record CommentUpdateRequest(
        @Schema(description = "수정할 댓글 내용", example = "This is an updated comment.")
        @NotBlank(message = REQUIRED_COMMENT_CONTENT)
        String content
) {}