package com.devon.techblog.domain.post.repository;

import com.devon.techblog.domain.post.entity.File;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileRepository extends JpaRepository<File, Long> {

    Optional<File> findByPostId(Long postId);

}
