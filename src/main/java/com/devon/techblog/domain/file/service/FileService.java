package com.devon.techblog.domain.file.service;

import com.devon.techblog.common.exception.CustomException;
import com.devon.techblog.common.exception.code.FileErrorCode;
import com.devon.techblog.domain.file.entity.File;
import com.devon.techblog.domain.file.entity.FileType;
import com.devon.techblog.domain.file.repository.FileRepository;
import com.devon.techblog.infra.image.ImageSignature;
import com.devon.techblog.infra.image.ImageStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FileService {

    private final FileRepository fileRepository;
    private final ImageStorageService imageStorageService;

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
                .orElseThrow(() -> new CustomException(FileErrorCode.FILE_NOT_FOUND));
    }

    public File getFileByStorageKey(String storageKey) {
        return fileRepository.findByStorageKey(storageKey)
                .orElseThrow(() -> new CustomException(FileErrorCode.FILE_NOT_FOUND));
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
    public void deleteFileByUrl(String url) {
        fileRepository.findByUrl(url).ifPresent(File::delete);
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

    @Transactional
    public PresignResult presignUpload(
            FileType fileType,
            String originalName,
            String mimeType
    ) {
        String storageKey = generateStorageKey(fileType, originalName);

        File file = File.createPending(fileType, originalName, storageKey, mimeType);
        File savedFile = fileRepository.save(file);

        String type = fileType == FileType.IMAGE ? "post" : "post";
        ImageSignature signature = imageStorageService.generateUploadSignature(type);

        return new PresignResult(savedFile.getId(), storageKey, signature);
    }

    @Transactional
    public File completeUpload(Long fileId, String url, Long size) {
        File file = getFileById(fileId);
        file.completeUpload(url, size);
        return file;
    }

    private String generateStorageKey(FileType fileType, String originalName) {
        String uuid = UUID.randomUUID().toString();
        String extension = getExtension(originalName);
        String folder = getFolder(fileType);
        return folder + "/" + uuid + extension;
    }

    private String getExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0 && lastDot < filename.length() - 1) {
            return filename.substring(lastDot);
        }
        return "";
    }

    private String getFolder(FileType fileType) {
        return switch (fileType) {
            case IMAGE -> "images";
            case VIDEO -> "videos";
            case DOCUMENT -> "documents";
        };
    }

    public record PresignResult(
            Long fileId,
            String storageKey,
            ImageSignature signature
    ) {
    }
}
