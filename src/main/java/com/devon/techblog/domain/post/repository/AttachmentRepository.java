package com.devon.techblog.domain.post.repository;

import com.devon.techblog.domain.post.entity.Attachment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    Optional<Attachment> findByPostId(Long postId);

}
