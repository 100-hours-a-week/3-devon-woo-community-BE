package com.kakaotechbootcamp.community.application.comment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import static com.kakaotechbootcamp.community.common.validation.ValidationMessages.REQUIRED_COMMENT_CONTENT;
import static com.kakaotechbootcamp.community.common.validation.ValidationMessages.REQUIRED_MEMBER_ID;

@Schema(description = "댓글 수정 요청 DTO")
public record CommentUpdateRequest(
        @Schema(description = "수정할 댓글 내용", example = "This is an updated comment.")
        @NotBlank(message = REQUIRED_COMMENT_CONTENT)
        String content
) {}