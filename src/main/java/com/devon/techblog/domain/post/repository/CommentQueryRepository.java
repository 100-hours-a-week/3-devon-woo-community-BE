package com.devon.techblog.domain.post.repository;

import com.devon.techblog.domain.post.dto.CommentQueryDto;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CommentQueryRepository {

    /**
     * 특정 게시글의 댓글 목록 조회 (Projection 사용 - 필요한 필드만)
     * fetch join 대신 필요한 컬럼만 SELECT하여 N+1 문제 해결 및 성능 최적화
     */
    Page<CommentQueryDto> findByPostIdWithMemberAsDto(Long postId, Pageable pageable);

}
