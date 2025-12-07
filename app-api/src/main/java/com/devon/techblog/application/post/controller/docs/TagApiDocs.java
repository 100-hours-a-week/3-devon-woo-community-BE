package com.devon.techblog.application.post.controller.docs;

import com.devon.techblog.common.dto.api.ApiResponse;
import com.devon.techblog.domain.post.entity.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;

@io.swagger.v3.oas.annotations.tags.Tag(
        name = "Tag",
        description = "태그 관련 API"
)
public interface TagApiDocs {

    @Operation(
            summary = "인기 태그 조회",
            description = "usageCount 기준 상위 N개의 태그를 조회합니다."
    )
    ApiResponse<List<Tag>> getPopularTags(
            @Parameter(description = "조회할 태그 개수", example = "10")
            int limit
    );
}
