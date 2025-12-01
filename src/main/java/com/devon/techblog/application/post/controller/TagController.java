package com.devon.techblog.application.post.controller;

import com.devon.techblog.application.post.service.TagService;
import com.devon.techblog.common.dto.api.ApiResponse;
import com.devon.techblog.domain.post.entity.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@io.swagger.v3.oas.annotations.tags.Tag(name = "Tag", description = "태그 관련 API")
@RestController
@RequestMapping("/api/v1/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @Operation(summary = "인기 태그 조회", description = "usageCount 기준 상위 N개의 태그를 조회합니다.")
    @GetMapping("/popular")
    public ApiResponse<List<Tag>> getPopularTags(
            @Parameter(description = "조회할 태그 개수", example = "10")
            @RequestParam(defaultValue = "10") int limit
    ) {
        List<Tag> tags = tagService.getTopTags(limit);
        return ApiResponse.success(tags, "popular_tags_retrieved");
    }
}
