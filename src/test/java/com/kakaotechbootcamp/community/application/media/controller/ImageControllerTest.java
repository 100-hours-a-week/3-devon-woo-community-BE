package com.kakaotechbootcamp.community.application.media.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kakaotechbootcamp.community.config.annotation.ControllerWebMvcTest;
import com.kakaotechbootcamp.community.infra.image.ImageSignature;
import com.kakaotechbootcamp.community.infra.image.ImageStorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@ControllerWebMvcTest(ImageController.class)
class ImageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ImageStorageService imageStorageService;

    @Test
    @DisplayName("이미지 업로드 서명 생성 - 기본값(post)")
    void sign_defaultType() throws Exception {
        ImageSignature signature = new ImageSignature(
                "test-api-key",
                "test-cloud",
                1234567890L,
                "test-signature",
                "unsigned_preset",
                "posts"
        );

        when(imageStorageService.generateUploadSignature("post")).thenReturn(signature);

        mockMvc.perform(get("/api/v1/images/sign"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.apiKey").value("test-api-key"))
                .andExpect(jsonPath("$.data.cloudName").value("test-cloud"))
                .andExpect(jsonPath("$.data.timestamp").value(1234567890L))
                .andExpect(jsonPath("$.data.signature").value("test-signature"))
                .andExpect(jsonPath("$.data.uploadPreset").value("unsigned_preset"))
                .andExpect(jsonPath("$.data.folder").value("posts"));

        verify(imageStorageService).generateUploadSignature("post");
    }

    @Test
    @DisplayName("이미지 업로드 서명 생성 - profile 타입")
    void sign_profileType() throws Exception {
        ImageSignature signature = new ImageSignature(
                "test-api-key",
                "test-cloud",
                1234567890L,
                "test-signature",
                "unsigned_preset",
                "profiles"
        );

        when(imageStorageService.generateUploadSignature("profile")).thenReturn(signature);

        mockMvc.perform(get("/api/v1/images/sign")
                        .param("type", "profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.folder").value("profiles"));

        verify(imageStorageService).generateUploadSignature("profile");
    }

    @Test
    @DisplayName("이미지 업로드 서명 생성 - post 타입")
    void sign_postType() throws Exception {
        ImageSignature signature = new ImageSignature(
                "test-api-key",
                "test-cloud",
                1234567890L,
                "test-signature",
                "unsigned_preset",
                "posts"
        );

        when(imageStorageService.generateUploadSignature("post")).thenReturn(signature);

        mockMvc.perform(get("/api/v1/images/sign")
                        .param("type", "post"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.folder").value("posts"));

        verify(imageStorageService).generateUploadSignature("post");
    }

    @Test
    @DisplayName("응답에 모든 필드 포함")
    void sign_allFieldsIncluded() throws Exception {
        ImageSignature signature = new ImageSignature(
                "test-api-key",
                "test-cloud",
                1234567890L,
                "test-signature",
                "unsigned_preset",
                "posts"
        );

        when(imageStorageService.generateUploadSignature("post")).thenReturn(signature);

        mockMvc.perform(get("/api/v1/images/sign"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.apiKey").exists())
                .andExpect(jsonPath("$.data.cloudName").exists())
                .andExpect(jsonPath("$.data.timestamp").exists())
                .andExpect(jsonPath("$.data.signature").exists())
                .andExpect(jsonPath("$.data.uploadPreset").exists())
                .andExpect(jsonPath("$.data.folder").exists());
    }
}
