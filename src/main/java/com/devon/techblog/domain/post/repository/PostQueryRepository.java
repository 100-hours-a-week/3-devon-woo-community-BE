package com.devon.techblog.domain.post.repository;

import com.devon.techblog.domain.post.dto.PostSearchCondition;
import com.devon.techblog.domain.post.dto.PostSummaryQueryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PostQueryRepository {

    Page<PostSummaryQueryDto> searchPosts(PostSearchCondition condition, Pageable pageable);

}
