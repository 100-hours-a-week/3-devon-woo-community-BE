package com.devon.techblog.domain.file.service;

import com.devon.techblog.domain.file.entity.File;
import com.devon.techblog.domain.file.entity.FileType;
import com.devon.techblog.domain.file.repository.FileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FileService {

    private final FileRepository fileRepository;

    @Transactional
    public File createFile(
            FileType fileType,
            String originalName,
            String s3Key,
            String url,
            Long size,
            String mimeType,
            Integer width,
            Integer height,
            Integer duration
    ) {
        File file = File.create(
                fileType,
                originalName,
                s3Key,
                url,
                size,
                mimeType,
                width,
                height,
                duration
        );
        return fileRepository.save(file);
    }

    public File getFileById(Long id) {
        return fileRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("file not found: " + id));
    }

    public File getFileByS3Key(String s3Key) {
        return fileRepository.findByS3Key(s3Key)
                .orElseThrow(() -> new IllegalArgumentException("file not found with s3Key: " + s3Key));
    }

    public List<File> getFilesByType(FileType fileType) {
        return fileRepository.findByFileType(fileType);
    }

    public List<File> getAllFiles() {
        return fileRepository.findAll();
    }

    @Transactional
    public void deleteFile(Long id) {
        File file = getFileById(id);
        file.delete();
    }

    @Transactional
    public void restoreFile(Long id) {
        File file = getFileById(id);
        file.restore();
    }

    @Transactional
    public void permanentlyDeleteFile(Long id) {
        File file = getFileById(id);
        fileRepository.delete(file);
    }

    @Transactional
    public void permanentlyDeleteFileByS3Key(String s3Key) {
        File file = getFileByS3Key(s3Key);
        fileRepository.delete(file);
    }
}
