package com.devon.techblog.domain.post.repository;

import com.devon.techblog.domain.post.entity.Series;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SeriesRepository extends JpaRepository<Series, Long> {

    @Query("SELECT s FROM Series s WHERE s.member.id = :memberId AND s.isDeleted = false")
    List<Series> findByMemberId(@Param("memberId") Long memberId);

    @Query("SELECT s FROM Series s WHERE s.id = :id AND s.isDeleted = false")
    Optional<Series> findByIdAndNotDeleted(@Param("id") Long id);
}
