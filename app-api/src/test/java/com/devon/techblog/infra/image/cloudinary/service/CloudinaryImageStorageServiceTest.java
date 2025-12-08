package com.devon.techblog.infra.image.cloudinary.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.devon.techblog.infra.image.ImageSignature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class CloudinaryImageStorageServiceTest {

    private CloudinaryImageStorageService createService() {
        CloudinaryImageStorageService service = new CloudinaryImageStorageService();
        ReflectionTestUtils.setField(service, "apiKey", "api-key");
        ReflectionTestUtils.setField(service, "apiSecret", "api-secret");
        ReflectionTestUtils.setField(service, "cloudName", "cloud-name");
        ReflectionTestUtils.setField(service, "uploadPreset", "unsigned");
        return service;
    }

    @Test
    @DisplayName("요청된 폴더 명으로 업로드 서명을 생성한다")
    void generateUploadSignature_usesRequestedFolder() {
        CloudinaryImageStorageService service = createService();

        ImageSignature signature = service.generateUploadSignature("images");

        assertThat(signature.folder()).isEqualTo("images");
        assertThat(signature.apiKey()).isEqualTo("api-key");
        assertThat(signature.uploadPreset()).isEqualTo("unsigned");
    }

    @Test
    @DisplayName("폴더 값이 비어있으면 기본 uploads 폴더를 사용한다")
    void generateUploadSignature_defaultFolderWhenBlank() {
        CloudinaryImageStorageService service = createService();

        ImageSignature signature = service.generateUploadSignature("   ");

        assertThat(signature.folder()).isEqualTo("uploads");
    }
}
