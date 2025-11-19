package com.kakaotechbootcamp.community.application.comment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import static com.kakaotechbootcamp.community.common.validation.ValidationMessages.REQUIRED_COMMENT_CONTENT;
import static com.kakaotechbootcamp.community.common.validation.ValidationMessages.REQUIRED_MEMBER_ID;

@Schema(description = "댓글 생성 요청 DTO")
public record CommentCreateRequest(
        @Schema(description = "댓글 내용", example = "This is a comment.")
        @NotBlank(message = REQUIRED_COMMENT_CONTENT)
        String content
) {}