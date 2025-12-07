package com.devon.techblog.application.file.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.devon.techblog.application.file.FileRequestFixture;
import com.devon.techblog.application.file.dto.request.CompleteUploadRequest;
import com.devon.techblog.application.file.dto.request.FileCreateRequest;
import com.devon.techblog.application.file.dto.request.PresignRequest;
import com.devon.techblog.application.file.dto.response.FileResponse;
import com.devon.techblog.application.file.dto.response.PresignResponse;
import com.devon.techblog.common.exception.BusinessException;
import com.devon.techblog.common.exception.code.FileErrorCode;
import com.devon.techblog.config.annotation.UnitTest;
import com.devon.techblog.domain.file.FileFixture;
import com.devon.techblog.domain.file.entity.File;
import com.devon.techblog.domain.file.entity.FileType;
import com.devon.techblog.domain.file.repository.FileRepository;
import com.devon.techblog.infra.image.ImageSignature;
import com.devon.techblog.infra.image.ImageStorageService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@UnitTest
class FileServiceTest {

    @Mock
    private FileRepository fileRepository;

    @Mock
    private ImageStorageService imageStorageService;

    @InjectMocks
    private FileService fileService;

    @Test
    @DisplayName("파일을 생성할 수 있다")
    void createFile_success() {
        File file = FileFixture.createWithId(1L);
        given(fileRepository.save(any(File.class))).willReturn(file);

        FileCreateRequest request = FileRequestFixture.createRequest();

        FileResponse response = fileService.createFile(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.fileType()).isEqualTo(FileType.IMAGE);
        assertThat(response.originalName()).isEqualTo(FileFixture.DEFAULT_ORIGINAL_NAME);
        verify(fileRepository, times(1)).save(any(File.class));
    }

    @Test
    @DisplayName("ID로 파일을 조회할 수 있다")
    void getFileById_success() {
        File file = FileFixture.createWithId(1L);
        given(fileRepository.findById(1L)).willReturn(Optional.of(file));

        FileResponse response = fileService.getFileById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.storageKey()).isEqualTo(FileFixture.DEFAULT_STORAGE_KEY);
        verify(fileRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("존재하지 않는 ID로 조회 시 예외가 발생한다")
    void getFileById_notFound_throwsException() {
        given(fileRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> fileService.getFileById(1L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", FileErrorCode.FILE_NOT_FOUND);
    }

    @Test
    @DisplayName("storageKey로 파일을 조회할 수 있다")
    void getFileByStorageKey_success() {
        File file = FileFixture.createWithId(1L);
        given(fileRepository.findByStorageKey(FileFixture.DEFAULT_STORAGE_KEY))
                .willReturn(Optional.of(file));

        FileResponse response = fileService.getFileByStorageKey(FileFixture.DEFAULT_STORAGE_KEY);

        assertThat(response.storageKey()).isEqualTo(FileFixture.DEFAULT_STORAGE_KEY);
        verify(fileRepository, times(1)).findByStorageKey(FileFixture.DEFAULT_STORAGE_KEY);
    }

    @Test
    @DisplayName("존재하지 않는 storageKey로 조회 시 예외가 발생한다")
    void getFileByStorageKey_notFound_throwsException() {
        given(fileRepository.findByStorageKey("nonexistent-key")).willReturn(Optional.empty());

        assertThatThrownBy(() -> fileService.getFileByStorageKey("nonexistent-key"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", FileErrorCode.FILE_NOT_FOUND);
    }

    @Test
    @DisplayName("파일 타입으로 파일 목록을 조회할 수 있다")
    void getFilesByType_success() {
        File image1 = FileFixture.createWithId(1L);
        File image2 = FileFixture.createWithId(2L);
        given(fileRepository.findByFileType(FileType.IMAGE))
                .willReturn(List.of(image1, image2));

        List<FileResponse> responses = fileService.getFilesByType(FileType.IMAGE);

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(FileResponse::id).containsExactly(1L, 2L);
        verify(fileRepository, times(1)).findByFileType(FileType.IMAGE);
    }

    @Test
    @DisplayName("전체 파일 목록을 조회할 수 있다")
    void getAllFiles_success() {
        File file1 = FileFixture.createWithId(1L);
        File file2 = FileFixture.createWithId(2L);
        File file3 = FileFixture.createWithId(3L);
        given(fileRepository.findAll()).willReturn(List.of(file1, file2, file3));

        List<FileResponse> responses = fileService.getAllFiles();

        assertThat(responses).hasSize(3);
        verify(fileRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("파일을 삭제 처리할 수 있다")
    void deleteFile_success() {
        File file = FileFixture.createWithId(1L);
        given(fileRepository.findById(1L)).willReturn(Optional.of(file));

        fileService.deleteFile(1L);

        assertThat(file.isDeleted()).isTrue();
        verify(fileRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("파일을 복구할 수 있다")
    void restoreFile_success() {
        File file = FileFixture.createWithId(1L);
        file.delete();
        given(fileRepository.findById(1L)).willReturn(Optional.of(file));

        fileService.restoreFile(1L);

        assertThat(file.isDeleted()).isFalse();
        verify(fileRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("파일을 영구 삭제할 수 있다")
    void permanentlyDeleteFile_success() {
        File file = FileFixture.createWithId(1L);
        given(fileRepository.findById(1L)).willReturn(Optional.of(file));

        fileService.permanentlyDeleteFile(1L);

        verify(fileRepository, times(1)).findById(1L);
        verify(fileRepository, times(1)).delete(file);
    }

    @Test
    @DisplayName("Presigned URL을 발급할 수 있다")
    void presignUpload_success() {
        File file = FileFixture.createWithId(1L);
        ImageSignature signature = new ImageSignature(
                "api-key",
                "cloud-name",
                1234567890L,
                "signature",
                "preset",
                "images"
        );
        given(fileRepository.save(any(File.class))).willReturn(file);
        given(imageStorageService.generateUploadSignature("images")).willReturn(signature);

        PresignRequest request = new PresignRequest(
                FileType.IMAGE,
                "test.jpg",
                "image/jpeg"
        );

        PresignResponse response = fileService.presignUpload(request);

        assertThat(response.fileId()).isEqualTo(1L);
        assertThat(response.storageKey()).contains("images/");
        assertThat(response.uploadSignature().apiKey()).isEqualTo("api-key");
        verify(fileRepository, times(1)).save(any(File.class));
        verify(imageStorageService, times(1)).generateUploadSignature("images");
    }

    @Test
    @DisplayName("파일 업로드를 완료할 수 있다")
    void completeUpload_success() {
        File file = FileFixture.createWithId(1L);
        given(fileRepository.findById(1L)).willReturn(Optional.of(file));

        CompleteUploadRequest request = new CompleteUploadRequest(
                "https://example.com/uploaded.jpg",
                2048L
        );

        FileResponse response = fileService.completeUpload(1L, request);

        assertThat(response.url()).isEqualTo("https://example.com/uploaded.jpg");
        assertThat(response.size()).isEqualTo(2048L);
        verify(fileRepository, times(1)).findById(1L);
    }
}
