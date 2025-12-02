package com.devon.techblog.domain.common.repository;

import com.devon.techblog.domain.common.entity.File;
import com.devon.techblog.domain.common.enums.FileType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FileRepository extends JpaRepository<File, Long> {

    List<File> findByFileType(FileType fileType);

    Optional<File> findByS3Key(String s3Key);
}
