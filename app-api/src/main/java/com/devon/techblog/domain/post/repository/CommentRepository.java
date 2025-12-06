package com.devon.techblog.domain.post.repository;

import com.devon.techblog.domain.post.entity.Comment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, Long>, CommentQueryRepository {

    @Query("SELECT c FROM Comment c JOIN FETCH c.member WHERE c.id = :commentId")
    Optional<Comment> findByIdWithMember(@Param("commentId") Long commentId);

    @Query("SELECT c.post.id FROM Comment c WHERE c.id = :commentId")
    Optional<Long> findPostIdByCommentId(@Param("commentId") Long commentId);

    @Query("SELECT c FROM Comment c JOIN FETCH c.post WHERE c.member.id = :memberId ORDER BY c.createdAt DESC")
    List<Comment> findByMemberIdWithPost(@Param("memberId") Long memberId);
}
