package com.devon.techblog.domain.post.repository;

import com.devon.techblog.domain.post.entity.PostTag;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostTagRepository extends JpaRepository<PostTag, Long> {

    @Query("SELECT pt FROM PostTag pt JOIN FETCH pt.tag WHERE pt.post.id = :postId")
    List<PostTag> findByPostIdWithTag(@Param("postId") Long postId);

    void deleteByPostId(Long postId);
}
