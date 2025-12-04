package com.devon.techblog.domain.post.repository;

import com.devon.techblog.domain.post.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    @Query("SELECT CASE WHEN COUNT(pl) > 0 THEN true ELSE false END FROM PostLike pl WHERE pl.id.postId = :postId AND pl.id.memberId = :memberId")
    boolean existsByPostIdAndMemberId(@Param("postId") Long postId, @Param("memberId") Long memberId);

    void deleteByPostIdAndMemberId(Long postId, Long memberId);

    long countByPostId(Long postId);
}
