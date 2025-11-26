package com.kakaotechbootcamp.community.domain.post.repository;

import com.kakaotechbootcamp.community.domain.post.entity.Attachment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    Optional<Attachment> findByPostId(Long postId);

}
