package com.kakaotechbootcamp.community.infra.image.cloudinary;

import com.kakaotechbootcamp.community.infra.image.ImageSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class CloudinaryImageStorageServiceTest {

    private CloudinaryImageStorageService imageStorageService;

    @BeforeEach
    void setUp() {
        imageStorageService = new CloudinaryImageStorageService();
        ReflectionTestUtils.setField(imageStorageService, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(imageStorageService, "apiSecret", "test-api-secret");
        ReflectionTestUtils.setField(imageStorageService, "cloudName", "test-cloud");
        ReflectionTestUtils.setField(imageStorageService, "uploadPreset", "unsigned_preset");
    }

    @Test
    @DisplayName("프로필 이미지 업로드 서명 생성")
    void generateUploadSignature_profile() {
        ImageSignature signature = imageStorageService.generateUploadSignature("profile");

        assertThat(signature).isNotNull();
        assertThat(signature.apiKey()).isEqualTo("test-api-key");
        assertThat(signature.cloudName()).isEqualTo("test-cloud");
        assertThat(signature.timestamp()).isNotNull();
        assertThat(signature.signature()).isNotNull();
        assertThat(signature.uploadPreset()).isEqualTo("unsigned_preset");
        assertThat(signature.folder()).isEqualTo("profiles");
    }

    @Test
    @DisplayName("게시글 이미지 업로드 서명 생성")
    void generateUploadSignature_post() {
        ImageSignature signature = imageStorageService.generateUploadSignature("post");

        assertThat(signature).isNotNull();
        assertThat(signature.apiKey()).isEqualTo("test-api-key");
        assertThat(signature.cloudName()).isEqualTo("test-cloud");
        assertThat(signature.timestamp()).isNotNull();
        assertThat(signature.signature()).isNotNull();
        assertThat(signature.uploadPreset()).isEqualTo("unsigned_preset");
        assertThat(signature.folder()).isEqualTo("posts");
    }

    @Test
    @DisplayName("알 수 없는 타입 - 기본값 posts 폴더")
    void generateUploadSignature_unknownType() {
        ImageSignature signature = imageStorageService.generateUploadSignature("unknown");

        assertThat(signature.folder()).isEqualTo("posts");
    }

    @Test
    @DisplayName("서명 생성 시 타임스탬프는 현재 시간")
    void generateUploadSignature_timestamp() {
        long beforeTimestamp = System.currentTimeMillis() / 1000L;
        ImageSignature signature = imageStorageService.generateUploadSignature("post");
        long afterTimestamp = System.currentTimeMillis() / 1000L;

        assertThat(signature.timestamp()).isBetween(beforeTimestamp, afterTimestamp);
    }

    @Test
    @DisplayName("서명은 SHA1 해시값")
    void generateUploadSignature_signatureFormat() {
        ImageSignature signature = imageStorageService.generateUploadSignature("post");

        assertThat(signature.signature()).hasSize(40);
        assertThat(signature.signature()).matches("^[a-f0-9]{40}$");
    }

    @Test
    @DisplayName("동일한 설정으로 서명 생성 시 일관성 검증")
    void generateUploadSignature_consistency() {
        CloudinaryImageStorageService service1 = new CloudinaryImageStorageService();
        ReflectionTestUtils.setField(service1, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(service1, "apiSecret", "test-api-secret");
        ReflectionTestUtils.setField(service1, "cloudName", "test-cloud");
        ReflectionTestUtils.setField(service1, "uploadPreset", "unsigned_preset");

        CloudinaryImageStorageService service2 = new CloudinaryImageStorageService();
        ReflectionTestUtils.setField(service2, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(service2, "apiSecret", "test-api-secret");
        ReflectionTestUtils.setField(service2, "cloudName", "test-cloud");
        ReflectionTestUtils.setField(service2, "uploadPreset", "unsigned_preset");

        ImageSignature sig1 = service1.generateUploadSignature("post");
        ImageSignature sig2 = service2.generateUploadSignature("post");

        assertThat(sig1.apiKey()).isEqualTo(sig2.apiKey());
        assertThat(sig1.cloudName()).isEqualTo(sig2.cloudName());
        assertThat(sig1.uploadPreset()).isEqualTo(sig2.uploadPreset());
    }

    @Test
    @DisplayName("모든 필드가 null이 아님")
    void generateUploadSignature_allFieldsNotNull() {
        ImageSignature signature = imageStorageService.generateUploadSignature("post");

        assertThat(signature.apiKey()).isNotNull();
        assertThat(signature.cloudName()).isNotNull();
        assertThat(signature.timestamp()).isNotNull();
        assertThat(signature.signature()).isNotNull();
        assertThat(signature.uploadPreset()).isNotNull();
        assertThat(signature.folder()).isNotNull();
    }
}
