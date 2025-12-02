package com.devon.techblog.domain.file.repository;

import com.devon.techblog.domain.file.entity.File;
import com.devon.techblog.domain.file.entity.FileType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileRepository extends JpaRepository<File, Long> {

    List<File> findByFileType(FileType fileType);

    Optional<File> findByStorageKey(String storageKey);
}
