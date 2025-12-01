package com.devon.techblog.domain.post.repository;

import com.devon.techblog.domain.post.entity.Tag;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findByName(String name);

    @Query("SELECT t FROM Tag t WHERE t.name IN :names")
    List<Tag> findByNameIn(@Param("names") List<String> names);

    @Query("SELECT t FROM Tag t ORDER BY t.usageCount DESC LIMIT :limit")
    List<Tag> findTopByUsageCount(@Param("limit") int limit);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Tag t SET t.usageCount = t.usageCount + 1 WHERE t.id = :tagId")
    int incrementUsageCount(@Param("tagId") Long tagId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Tag t SET t.usageCount = t.usageCount - 1 WHERE t.id = :tagId AND t.usageCount > 0")
    int decrementUsageCount(@Param("tagId") Long tagId);
}
