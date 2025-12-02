package com.devon.techblog.domain.file.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.devon.techblog.config.annotation.RepositoryJpaTest;
import com.devon.techblog.domain.file.FileFixture;
import com.devon.techblog.domain.file.entity.File;
import com.devon.techblog.domain.file.entity.FileType;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@RepositoryJpaTest
class FileRepositoryTest {

    @Autowired
    private FileRepository fileRepository;

    @AfterEach
    void tearDown() {
        fileRepository.deleteAll();
    }

    @Test
    @DisplayName("파일을 저장하고 조회할 수 있다")
    void saveAndFind() {
        File file = File.create(
                FileType.IMAGE,
                "test.jpg",
                "uploads/test.jpg",
                "https://example.com/test.jpg",
                1024L,
                "image/jpeg"
        );

        File saved = fileRepository.save(file);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getFileType()).isEqualTo(FileType.IMAGE);
        assertThat(saved.getOriginalName()).isEqualTo("test.jpg");
        assertThat(saved.getStorageKey()).isEqualTo("uploads/test.jpg");
        assertThat(saved.getUrl()).isEqualTo("https://example.com/test.jpg");
        assertThat(saved.getSize()).isEqualTo(1024L);
        assertThat(saved.getMimeType()).isEqualTo("image/jpeg");
        assertThat(saved.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("storageKey로 파일을 조회할 수 있다")
    void findByStorageKey() {
        File file = FileFixture.create();
        fileRepository.save(file);

        Optional<File> found = fileRepository.findByStorageKey(FileFixture.DEFAULT_STORAGE_KEY);

        assertThat(found).isPresent();
        assertThat(found.get().getStorageKey()).isEqualTo(FileFixture.DEFAULT_STORAGE_KEY);
        assertThat(found.get().getOriginalName()).isEqualTo(FileFixture.DEFAULT_ORIGINAL_NAME);
    }

    @Test
    @DisplayName("존재하지 않는 storageKey로 조회하면 빈 Optional을 반환한다")
    void findByStorageKey_notFound() {
        Optional<File> found = fileRepository.findByStorageKey("nonexistent-key");

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("파일 타입으로 파일을 조회할 수 있다")
    void findByFileType() {
        fileRepository.save(FileFixture.createImage());
        fileRepository.save(FileFixture.createVideo());
        fileRepository.save(FileFixture.createDocument());
        fileRepository.save(FileFixture.create(
                FileType.IMAGE,
                "another.png",
                "uploads/another.png",
                "https://example.com/another.png",
                2048L,
                "image/png"
        ));

        List<File> images = fileRepository.findByFileType(FileType.IMAGE);
        List<File> videos = fileRepository.findByFileType(FileType.VIDEO);
        List<File> documents = fileRepository.findByFileType(FileType.DOCUMENT);

        assertThat(images).hasSize(2);
        assertThat(videos).hasSize(1);
        assertThat(documents).hasSize(1);
        assertThat(images).allMatch(file -> file.getFileType() == FileType.IMAGE);
        assertThat(videos).allMatch(file -> file.getFileType() == FileType.VIDEO);
    }

    @Test
    @DisplayName("파일을 삭제 처리할 수 있다")
    void deleteFile() {
        File file = fileRepository.save(FileFixture.create());

        file.delete();
        fileRepository.save(file);

        File found = fileRepository.findById(file.getId()).orElseThrow();
        assertThat(found.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("삭제된 파일을 복구할 수 있다")
    void restoreFile() {
        File file = fileRepository.save(FileFixture.create());
        file.delete();
        fileRepository.save(file);

        file.restore();
        fileRepository.save(file);

        File found = fileRepository.findById(file.getId()).orElseThrow();
        assertThat(found.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("여러 파일을 저장하고 전체 조회할 수 있다")
    void saveMultipleAndFindAll() {
        fileRepository.save(FileFixture.createImage());
        fileRepository.save(FileFixture.createVideo());
        fileRepository.save(FileFixture.createDocument());

        List<File> files = fileRepository.findAll();

        assertThat(files).hasSize(3);
        assertThat(files).extracting(File::getFileType)
                .containsExactlyInAnyOrder(FileType.IMAGE, FileType.VIDEO, FileType.DOCUMENT);
    }

    @Test
    @DisplayName("새로운 필드들이 저장되고 조회된다")
    void saveAndFind_newFields() {
        File file = File.create(
                FileType.VIDEO,
                "vacation.mp4",
                "uploads/2024/vacation-uuid.mp4",
                "https://cdn.example.com/uploads/2024/vacation-uuid.mp4",
                10485760L,
                "video/mp4"
        );

        File saved = fileRepository.save(file);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStorageKey()).isEqualTo("uploads/2024/vacation-uuid.mp4");
        assertThat(saved.getUrl()).isEqualTo("https://cdn.example.com/uploads/2024/vacation-uuid.mp4");
        assertThat(saved.getSize()).isEqualTo(10485760L);
    }
}
