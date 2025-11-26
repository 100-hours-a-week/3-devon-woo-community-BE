package com.kakaotechbootcamp.community.application.common.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.data.domain.Page;

@Schema(description = "페이징 응답 공통 DTO")
public record PageResponse<T>(
        @Schema(description = "데이터 목록")
        List<T> items,

        @Schema(description = "현재 페이지 번호 (0부터 시작)", example = "0")
        int page,

        @Schema(description = "페이지 크기", example = "20")
        int size,

        @Schema(description = "전체 항목 수", example = "100")
        long totalElements,

        @Schema(description = "전체 페이지 수", example = "5")
        int totalPages
) {
    public static <T> PageResponse<T> of(List<T> items, Page<?> page) {
        return new PageResponse<>(
                items,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
