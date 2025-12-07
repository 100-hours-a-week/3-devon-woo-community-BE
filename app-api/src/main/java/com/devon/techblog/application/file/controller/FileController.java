package com.devon.techblog.application.file.controller;

import com.devon.techblog.application.file.controller.docs.FileApiDocs;
import com.devon.techblog.application.file.dto.request.CompleteUploadRequest;
import com.devon.techblog.application.file.dto.request.FileCreateRequest;
import com.devon.techblog.application.file.dto.request.PresignRequest;
import com.devon.techblog.application.file.dto.response.FileResponse;
import com.devon.techblog.application.file.dto.response.PresignResponse;
import com.devon.techblog.application.file.service.FileService;
import com.devon.techblog.common.dto.api.ApiResponse;
import com.devon.techblog.domain.file.entity.FileType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController implements FileApiDocs {

    private final FileService fileService;

    @PostMapping("/presign")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PresignResponse> presignUpload(
            @RequestBody @Validated PresignRequest request
    ) {
        PresignResponse response = fileService.presignUpload(request);
        return ApiResponse.success(response);
    }

    @PostMapping("/{fileId}/complete")
    public ApiResponse<FileResponse> completeUpload(
            @PathVariable Long fileId,
            @RequestBody @Validated CompleteUploadRequest request
    ) {
        FileResponse response = fileService.completeUpload(fileId, request);
        return ApiResponse.success(response);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<FileResponse> createFile(
            @RequestBody @Validated FileCreateRequest request
    ) {
        FileResponse response = fileService.createFile(request);
        return ApiResponse.success(response);
    }

    @GetMapping("/{id}")
    public ApiResponse<FileResponse> getFile(
            @PathVariable Long id
    ) {
        FileResponse response = fileService.getFileById(id);
        return ApiResponse.success(response);
    }

    @GetMapping("/by-key")
    public ApiResponse<FileResponse> getFileByStorageKey(
            @RequestParam String storageKey
    ) {
        FileResponse response = fileService.getFileByStorageKey(storageKey);
        return ApiResponse.success(response);
    }

    @GetMapping
    public ApiResponse<List<FileResponse>> getAllFiles(
            @RequestParam(required = false) FileType fileType
    ) {
        List<FileResponse> responses = fileType != null
                ? fileService.getFilesByType(fileType)
                : fileService.getAllFiles();

        return ApiResponse.success(responses);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFile(
            @PathVariable Long id
    ) {
        fileService.deleteFile(id);
    }

    @PatchMapping("/{id}/restore")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void restoreFile(
            @PathVariable Long id
    ) {
        fileService.restoreFile(id);
    }

    @DeleteMapping("/{id}/permanent")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void permanentlyDeleteFile(
            @PathVariable Long id
    ) {
        fileService.permanentlyDeleteFile(id);
    }
}
