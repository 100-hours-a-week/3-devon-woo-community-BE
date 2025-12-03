package com.devon.techblog.domain.file.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.devon.techblog.common.exception.CustomException;
import com.devon.techblog.common.exception.code.FileErrorCode;
import com.devon.techblog.config.annotation.UnitTest;
import com.devon.techblog.domain.file.FileFixture;
import com.devon.techblog.domain.file.entity.File;
import com.devon.techblog.domain.file.entity.FileType;
import com.devon.techblog.domain.file.repository.FileRepository;
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

    @InjectMocks
    private FileService fileService;

    @Test
    @DisplayName("파일을 생성할 수 있다")
    void createFile_success() {
        File file = FileFixture.createWithId(
                1L,
                FileType.IMAGE,
                "test.jpg",
                "uploads/test.jpg",
                "https://example.com/test.jpg",
                1024L,
                "image/jpeg"
        );
        given(fileRepository.save(any(File.class))).willReturn(file);

        File created = fileService.createFile(
                FileType.IMAGE,
                "test.jpg",
                "uploads/test.jpg",
                "https://example.com/test.jpg",
                1024L,
                "image/jpeg"
        );

        assertThat(created.getId()).isEqualTo(1L);
        assertThat(created.getFileType()).isEqualTo(FileType.IMAGE);
        assertThat(created.getOriginalName()).isEqualTo("test.jpg");
        verify(fileRepository, times(1)).save(any(File.class));
    }

    @Test
    @DisplayName("ID로 파일을 조회할 수 있다")
    void getFileById_success() {
        File file = FileFixture.createWithId(1L);
        given(fileRepository.findById(1L)).willReturn(Optional.of(file));

        File found = fileService.getFileById(1L);

        assertThat(found.getId()).isEqualTo(1L);
        assertThat(found.getStorageKey()).isEqualTo(FileFixture.DEFAULT_STORAGE_KEY);
        verify(fileRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("존재하지 않는 ID로 조회 시 예외가 발생한다")
    void getFileById_notFound_throwsException() {
        given(fileRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> fileService.getFileById(1L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", FileErrorCode.FILE_NOT_FOUND);
    }

    @Test
    @DisplayName("storageKey로 파일을 조회할 수 있다")
    void getFileByStorageKey_success() {
        File file = FileFixture.createWithId(1L);
        given(fileRepository.findByStorageKey(FileFixture.DEFAULT_STORAGE_KEY))
                .willReturn(Optional.of(file));

        File found = fileService.getFileByStorageKey(FileFixture.DEFAULT_STORAGE_KEY);

        assertThat(found.getStorageKey()).isEqualTo(FileFixture.DEFAULT_STORAGE_KEY);
        verify(fileRepository, times(1)).findByStorageKey(FileFixture.DEFAULT_STORAGE_KEY);
    }

    @Test
    @DisplayName("존재하지 않는 storageKey로 조회 시 예외가 발생한다")
    void getFileByStorageKey_notFound_throwsException() {
        given(fileRepository.findByStorageKey("nonexistent-key")).willReturn(Optional.empty());

        assertThatThrownBy(() -> fileService.getFileByStorageKey("nonexistent-key"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", FileErrorCode.FILE_NOT_FOUND);
    }

    @Test
    @DisplayName("파일 타입으로 파일 목록을 조회할 수 있다")
    void getFilesByType_success() {
        File image1 = FileFixture.createWithId(1L);
        File image2 = FileFixture.createWithId(2L);
        given(fileRepository.findByFileType(FileType.IMAGE))
                .willReturn(List.of(image1, image2));

        List<File> files = fileService.getFilesByType(FileType.IMAGE);

        assertThat(files).hasSize(2);
        assertThat(files).extracting(File::getId).containsExactly(1L, 2L);
        verify(fileRepository, times(1)).findByFileType(FileType.IMAGE);
    }

    @Test
    @DisplayName("전체 파일 목록을 조회할 수 있다")
    void getAllFiles_success() {
        File file1 = FileFixture.createWithId(1L);
        File file2 = FileFixture.createWithId(2L);
        File file3 = FileFixture.createWithId(3L);
        given(fileRepository.findAll()).willReturn(List.of(file1, file2, file3));

        List<File> files = fileService.getAllFiles();

        assertThat(files).hasSize(3);
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
    @DisplayName("storageKey로 파일을 영구 삭제할 수 있다")
    void permanentlyDeleteFileByStorageKey_success() {
        File file = FileFixture.createWithId(1L);
        given(fileRepository.findByStorageKey(FileFixture.DEFAULT_STORAGE_KEY))
                .willReturn(Optional.of(file));

        fileService.permanentlyDeleteFileByStorageKey(FileFixture.DEFAULT_STORAGE_KEY);

        verify(fileRepository, times(1)).findByStorageKey(FileFixture.DEFAULT_STORAGE_KEY);
        verify(fileRepository, times(1)).delete(file);
    }
}
