package com.devon.techblog.application.file.controller;

import com.devon.techblog.application.file.dto.request.CompleteUploadRequest;
import com.devon.techblog.application.file.dto.request.FileCreateRequest;
import com.devon.techblog.application.file.dto.request.PresignRequest;
import com.devon.techblog.application.file.dto.response.FileResponse;
import com.devon.techblog.application.file.dto.response.PresignResponse;
import com.devon.techblog.common.dto.api.ApiResponse;
import com.devon.techblog.domain.file.entity.File;
import com.devon.techblog.domain.file.entity.FileType;
import com.devon.techblog.domain.file.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "File", description = "파일 관련 API")
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @Operation(summary = "파일 업로드를 위한 Presigned URL 발급", description = "파일 업로드를 위한 Presigned URL을 발급합니다.")
    @PostMapping("/presign")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PresignResponse> presignUpload(
            @RequestBody @Validated PresignRequest request
    ) {
        FileService.PresignResult result = fileService.presignUpload(
                request.fileType(),
                request.originalName(),
                request.mimeType()
        );

        PresignResponse response = new PresignResponse(
                result.fileId(),
                result.storageKey(),
                new PresignResponse.UploadSignature(
                        result.signature().apiKey(),
                        result.signature().cloudName(),
                        result.signature().timestamp(),
                        result.signature().signature(),
                        result.signature().uploadPreset(),
                        result.signature().folder()
                )
        );

        return ApiResponse.success(response, "presign_url_created");
    }

    @Operation(summary = "파일 업로드 완료", description = "파일 업로드가 완료되었음을 서버에 알립니다.")
    @PostMapping("/{fileId}/complete")
    public ApiResponse<FileResponse> completeUpload(
            @Parameter(description = "파일 ID") @PathVariable Long fileId,
            @RequestBody @Validated CompleteUploadRequest request
    ) {
        File file = fileService.completeUpload(fileId, request.url(), request.size());
        return ApiResponse.success(FileResponse.from(file), "upload_completed");
    }

    @Operation(summary = "파일 메타데이터 생성", description = "파일 메타데이터를 직접 생성합니다. (레거시)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<FileResponse> createFile(
            @RequestBody @Validated FileCreateRequest request
    ) {
        File file = fileService.createFile(
                request.fileType(),
                request.originalName(),
                request.storageKey(),
                request.url(),
                request.size(),
                request.mimeType()
        );
        return ApiResponse.success(FileResponse.from(file), "file_created");
    }

    @Operation(summary = "파일 조회", description = "파일 ID로 파일 정보를 조회합니다.")
    @GetMapping("/{id}")
    public ApiResponse<FileResponse> getFile(
            @Parameter(description = "파일 ID") @PathVariable Long id
    ) {
        File file = fileService.getFileById(id);
        return ApiResponse.success(FileResponse.from(file), "file_retrieved");
    }

    @Operation(summary = "storageKey로 파일 조회", description = "storageKey로 파일 정보를 조회합니다.")
    @GetMapping("/by-key")
    public ApiResponse<FileResponse> getFileByStorageKey(
            @Parameter(description = "Storage Key") @RequestParam String storageKey
    ) {
        File file = fileService.getFileByStorageKey(storageKey);
        return ApiResponse.success(FileResponse.from(file), "file_retrieved");
    }

    @Operation(summary = "파일 목록 조회", description = "전체 파일 목록을 조회합니다.")
    @GetMapping
    public ApiResponse<List<FileResponse>> getAllFiles(
            @Parameter(description = "파일 타입 필터") @RequestParam(required = false) FileType fileType
    ) {
        List<File> files = fileType != null
                ? fileService.getFilesByType(fileType)
                : fileService.getAllFiles();

        List<FileResponse> responses = files.stream()
                .map(FileResponse::from)
                .toList();

        return ApiResponse.success(responses, "files_retrieved");
    }

    @Operation(summary = "파일 삭제", description = "파일을 soft delete 처리합니다.")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFile(
            @Parameter(description = "파일 ID") @PathVariable Long id
    ) {
        fileService.deleteFile(id);
    }

    @Operation(summary = "파일 복구", description = "삭제된 파일을 복구합니다.")
    @PatchMapping("/{id}/restore")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void restoreFile(
            @Parameter(description = "파일 ID") @PathVariable Long id
    ) {
        fileService.restoreFile(id);
    }

    @Operation(summary = "파일 영구 삭제", description = "파일을 데이터베이스에서 영구 삭제합니다.")
    @DeleteMapping("/{id}/permanent")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void permanentlyDeleteFile(
            @Parameter(description = "파일 ID") @PathVariable Long id
    ) {
        fileService.permanentlyDeleteFile(id);
    }
}
