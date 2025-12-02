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
            String storageKey,
            String url,
            Long size,
            String mimeType
    ) {
        File file = File.create(
                fileType,
                originalName,
                storageKey,
                url,
                size,
                mimeType
        );
        return fileRepository.save(file);
    }

    public File getFileById(Long id) {
        return fileRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("file not found: " + id));
    }

    public File getFileByStorageKey(String storageKey) {
        return fileRepository.findByStorageKey(storageKey)
                .orElseThrow(() -> new IllegalArgumentException("file not found with storageKey: " + storageKey));
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
    public void permanentlyDeleteFileByStorageKey(String storageKey) {
        File file = getFileByStorageKey(storageKey);
        fileRepository.delete(file);
    }
}
