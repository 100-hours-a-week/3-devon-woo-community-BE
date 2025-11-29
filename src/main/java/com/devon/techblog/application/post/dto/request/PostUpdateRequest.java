package com.devon.techblog.application.post.dto.request;

import static com.devon.techblog.common.validation.ValidationMessages.INVALID_IMAGE_URL;
import static com.devon.techblog.common.validation.ValidationPatterns.URL_PATTERN;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import java.util.List;

@Schema(description = "게시글 수정 요청 DTO")
public record PostUpdateRequest(
        @Schema(description = "수정할 게시글 제목", example = "This is an updated title.")
        String title,

        @Schema(description = "수정할 게시글 내용", example = "This is an updated content.")
        String content,

        @Schema(description = "수정할 이미지 URL", example = "https://picsum.photos/300")
        @Pattern(regexp = URL_PATTERN, message = INVALID_IMAGE_URL)
        String image,

        @Schema(description = "썸네일 URL", example = "https://picsum.photos/200/150")
        @Pattern(regexp = URL_PATTERN, message = INVALID_IMAGE_URL)
        String thumbnail,

        @Schema(description = "요약", example = "게시글 요약입니다")
        String summary,

        @Schema(description = "태그 목록", example = "[\"Java\", \"Spring\"]")
        List<String> tags,

        @Schema(description = "시리즈 ID", example = "1")
        Long seriesId,

        @Schema(description = "공개 범위", example = "public")
        String visibility,

        @Schema(description = "댓글 허용 여부", example = "true")
        Boolean commentsAllowed
) {}