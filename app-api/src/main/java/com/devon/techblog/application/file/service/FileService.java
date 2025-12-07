package com.devon.techblog.application.file.service;

import com.devon.techblog.application.file.dto.request.CompleteUploadRequest;
import com.devon.techblog.application.file.dto.request.FileCreateRequest;
import com.devon.techblog.application.file.dto.request.PresignRequest;
import com.devon.techblog.application.file.dto.response.FileResponse;
import com.devon.techblog.application.file.dto.response.PresignResponse;
import com.devon.techblog.common.exception.BusinessException;
import com.devon.techblog.common.exception.code.FileErrorCode;
import com.devon.techblog.domain.file.entity.File;
import com.devon.techblog.domain.file.entity.FileType;
import com.devon.techblog.domain.file.repository.FileRepository;
import com.devon.techblog.domain.file.util.FileStorageKeyGenerator;
import com.devon.techblog.infra.image.ImageStorageService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FileService {

    private final FileRepository fileRepository;
    private final ImageStorageService imageStorageService;

    @Transactional
    public FileResponse createFile(FileCreateRequest request) {
        File file = File.create(
                request.fileType(),
                request.originalName(),
                request.storageKey(),
                request.url(),
                request.size(),
                request.mimeType()
        );
        File savedFile = fileRepository.save(file);
        return FileResponse.from(savedFile);
    }

    public FileResponse getFileById(Long id) {
        File file = fileRepository.findById(id)
                .orElseThrow(() -> new BusinessException(FileErrorCode.FILE_NOT_FOUND));
        return FileResponse.from(file);
    }

    public FileResponse getFileByStorageKey(String storageKey) {
        File file = fileRepository.findByStorageKey(storageKey)
                .orElseThrow(() -> new BusinessException(FileErrorCode.FILE_NOT_FOUND));
        return FileResponse.from(file);
    }

    public List<FileResponse> getFilesByType(FileType fileType) {
        return fileRepository.findByFileType(fileType)
                .stream()
                .map(FileResponse::from)
                .toList();
    }

    public List<FileResponse> getAllFiles() {
        return fileRepository.findAll()
                .stream()
                .map(FileResponse::from)
                .toList();
    }

    @Transactional
    public void deleteFile(Long id) {
        File file = findFileById(id);
        file.delete();
    }

    @Transactional
    public void deleteFileByUrl(String url) {
        fileRepository.findByUrl(url).ifPresent(File::delete);
    }

    @Transactional
    public void restoreFile(Long id) {
        File file = findFileById(id);
        file.restore();
    }

    @Transactional
    public void permanentlyDeleteFile(Long id) {
        File file = findFileById(id);
        fileRepository.delete(file);
    }

    @Transactional
    public PresignResponse presignUpload(PresignRequest request) {
        String storageKey = FileStorageKeyGenerator.generate(
                request.fileType(),
                request.originalName()
        );

        File file = File.createPending(
                request.fileType(),
                request.originalName(),
                storageKey,
                request.mimeType()
        );
        File savedFile = fileRepository.save(file);

        return PresignResponse.from(
                savedFile.getId(),
                storageKey,
                imageStorageService.generateUploadSignature(request.fileType().getFolder())
        );
    }

    @Transactional
    public FileResponse completeUpload(Long fileId, CompleteUploadRequest request) {
        File file = findFileById(fileId);
        file.completeUpload(request.url(), request.size());
        return FileResponse.from(file);
    }

    private File findFileById(Long id) {
        return fileRepository.findById(id)
                .orElseThrow(() -> new BusinessException(FileErrorCode.FILE_NOT_FOUND));
    }
}
