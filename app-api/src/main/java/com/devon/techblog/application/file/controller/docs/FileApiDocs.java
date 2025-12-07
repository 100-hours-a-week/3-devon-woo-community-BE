package com.devon.techblog.application.file.controller.docs;

import com.devon.techblog.application.file.dto.request.CompleteUploadRequest;
import com.devon.techblog.application.file.dto.request.FileCreateRequest;
import com.devon.techblog.application.file.dto.request.PresignRequest;
import com.devon.techblog.application.file.dto.response.FileResponse;
import com.devon.techblog.application.file.dto.response.PresignResponse;
import com.devon.techblog.common.dto.api.ApiResponse;
import com.devon.techblog.domain.file.entity.FileType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;

@Tag(
        name = "File",
        description = "파일 관련 API"
)
public interface FileApiDocs {

    @Operation(
            summary = "파일 업로드를 위한 Presigned URL 발급",
            description = "파일 업로드를 위한 Presigned URL을 발급합니다."
    )
    ApiResponse<PresignResponse> presignUpload(
            PresignRequest request
    );

    @Operation(
            summary = "파일 업로드 완료",
            description = "파일 업로드가 완료되었음을 서버에 알립니다."
    )
    ApiResponse<FileResponse> completeUpload(
            @Parameter(description = "파일 ID") Long fileId,
            CompleteUploadRequest request
    );

    @Operation(
            summary = "파일 메타데이터 생성",
            description = "파일 메타데이터를 직접 생성합니다. (레거시)"
    )
    ApiResponse<FileResponse> createFile(
            FileCreateRequest request
    );

    @Operation(
            summary = "파일 조회",
            description = "파일 ID로 파일 정보를 조회합니다."
    )
    ApiResponse<FileResponse> getFile(
            @Parameter(description = "파일 ID") Long id
    );

    @Operation(
            summary = "storageKey로 파일 조회",
            description = "storageKey로 파일 정보를 조회합니다."
    )
    ApiResponse<FileResponse> getFileByStorageKey(
            @Parameter(description = "Storage Key") String storageKey
    );

    @Operation(
            summary = "파일 목록 조회",
            description = "전체 파일 목록을 조회합니다."
    )
    ApiResponse<List<FileResponse>> getAllFiles(
            @Parameter(description = "파일 타입 필터") FileType fileType
    );

    @Operation(
            summary = "파일 삭제",
            description = "파일을 soft delete 처리합니다."
    )
    void deleteFile(
            @Parameter(description = "파일 ID") Long id
    );

    @Operation(
            summary = "파일 복구",
            description = "삭제된 파일을 복구합니다."
    )
    void restoreFile(
            @Parameter(description = "파일 ID") Long id
    );

    @Operation(
            summary = "파일 영구 삭제",
            description = "파일을 데이터베이스에서 영구 삭제합니다."
    )
    void permanentlyDeleteFile(
            @Parameter(description = "파일 ID") Long id
    );
}

