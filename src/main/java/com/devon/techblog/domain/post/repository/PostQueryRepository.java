package com.devon.techblog.domain.post.repository;

import com.devon.techblog.domain.post.dto.PostSummaryQueryDto;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PostQueryRepository {

    /**
     * 삭제되지 않은 게시글 목록 조회 (Projection 사용 - 필요한 필드만)
     * fetch join 대신 필요한 컬럼만 SELECT하여 성능 최적화
     */
    Page<PostSummaryQueryDto> findAllActiveWithMemberAsDto(Pageable pageable);

    /**
     * 제목 또는 내용으로 게시글 검색 (Projection 사용 - 필요한 필드만)
     */
    Page<PostSummaryQueryDto> searchByTitleOrContent(String keyword, Pageable pageable);

    /**
     * 태그로 게시글 필터링 조회
     * @param tags 필터링할 태그 목록 (OR 조건 - 하나라도 포함하면 조회)
     */
    Page<PostSummaryQueryDto> findByTagsIn(List<String> tags, Pageable pageable);
}
