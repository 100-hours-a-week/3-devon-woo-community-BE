package com.devon.techblog.integration.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.devon.techblog.application.file.dto.request.CompleteUploadRequest;
import com.devon.techblog.application.file.dto.request.PresignRequest;
import com.devon.techblog.config.annotation.IntegrationTest;
import com.devon.techblog.domain.file.entity.File;
import com.devon.techblog.domain.file.entity.FileStatus;
import com.devon.techblog.domain.file.entity.FileType;
import com.devon.techblog.domain.file.repository.FileRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class FileUploadIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FileRepository fileRepository;

    @Test
    @DisplayName("통합 테스트 - Presigned URL 기반 파일 업로드 전체 플로우")
    void fileUploadFlow_withPresignedUrl_success() throws Exception {
        PresignRequest presignRequest = new PresignRequest(
                FileType.IMAGE,
                "profile-picture.jpg",
                "image/jpeg"
        );

        MvcResult presignResult = mockMvc.perform(post("/api/v1/files/presign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(presignRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fileId").value(notNullValue()))
                .andExpect(jsonPath("$.data.storageKey").value(notNullValue()))
                .andExpect(jsonPath("$.data.uploadSignature.apiKey").value(notNullValue()))
                .andExpect(jsonPath("$.data.uploadSignature.cloudName").value(notNullValue()))
                .andExpect(jsonPath("$.data.uploadSignature.timestamp").value(notNullValue()))
                .andExpect(jsonPath("$.data.uploadSignature.signature").value(notNullValue()))
                .andReturn();

        String responseBody = presignResult.getResponse().getContentAsString();
        Long fileId = objectMapper.readTree(responseBody).get("data").get("fileId").asLong();

        File pendingFile = fileRepository.findById(fileId).orElseThrow();
        assertThat(pendingFile.getStatus()).isEqualTo(FileStatus.PENDING);
        assertThat(pendingFile.getOriginalName()).isEqualTo("profile-picture.jpg");
        assertThat(pendingFile.getMimeType()).isEqualTo("image/jpeg");

        CompleteUploadRequest completeRequest = new CompleteUploadRequest(
                "https://res.cloudinary.com/demo/image/upload/v1234567890/images/abc123.jpg",
                102400L
        );

        mockMvc.perform(post("/api/v1/files/{fileId}/complete", fileId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completeRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(fileId))
                .andExpect(jsonPath("$.data.status").value("UPLOADED"))
                .andExpect(jsonPath("$.data.url").value("https://res.cloudinary.com/demo/image/upload/v1234567890/images/abc123.jpg"))
                .andExpect(jsonPath("$.data.size").value(102400));

        File uploadedFile = fileRepository.findById(fileId).orElseThrow();
        assertThat(uploadedFile.getStatus()).isEqualTo(FileStatus.UPLOADED);
        assertThat(uploadedFile.getUrl()).isEqualTo("https://res.cloudinary.com/demo/image/upload/v1234567890/images/abc123.jpg");
        assertThat(uploadedFile.getSize()).isEqualTo(102400L);
    }

    @Test
    @DisplayName("통합 테스트 - 파일 조회 시 업로드된 파일 정보를 반환한다")
    void getFile_returnsUploadedFileInfo() throws Exception {
        PresignRequest presignRequest = new PresignRequest(
                FileType.IMAGE,
                "test-image.png",
                "image/png"
        );

        MvcResult presignResult = mockMvc.perform(post("/api/v1/files/presign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(presignRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = presignResult.getResponse().getContentAsString();
        Long fileId = objectMapper.readTree(responseBody).get("data").get("fileId").asLong();

        CompleteUploadRequest completeRequest = new CompleteUploadRequest(
                "https://res.cloudinary.com/demo/image/upload/v1234567890/images/test.png",
                204800L
        );

        mockMvc.perform(post("/api/v1/files/{fileId}/complete", fileId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completeRequest)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/files/{id}", fileId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(fileId))
                .andExpect(jsonPath("$.data.fileType").value("IMAGE"))
                .andExpect(jsonPath("$.data.originalName").value("test-image.png"))
                .andExpect(jsonPath("$.data.mimeType").value("image/png"))
                .andExpect(jsonPath("$.data.status").value("UPLOADED"))
                .andExpect(jsonPath("$.data.url").value("https://res.cloudinary.com/demo/image/upload/v1234567890/images/test.png"))
                .andExpect(jsonPath("$.data.size").value(204800));
    }

    @Test
    @DisplayName("통합 테스트 - 마크다운 게시글에 이미지가 포함된 시나리오")
    void postWithMarkdownImage_scenario() throws Exception {
        PresignRequest presignRequest = new PresignRequest(
                FileType.IMAGE,
                "post-image.jpg",
                "image/jpeg"
        );

        MvcResult presignResult = mockMvc.perform(post("/api/v1/files/presign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(presignRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = presignResult.getResponse().getContentAsString();
        Long fileId = objectMapper.readTree(responseBody).get("data").get("fileId").asLong();
        String storageKey = objectMapper.readTree(responseBody).get("data").get("storageKey").asText();

        CompleteUploadRequest completeRequest = new CompleteUploadRequest(
                "https://res.cloudinary.com/demo/image/upload/" + storageKey,
                512000L
        );

        mockMvc.perform(post("/api/v1/files/{fileId}/complete", fileId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completeRequest)))
                .andExpect(status().isOk());

        File uploadedFile = fileRepository.findById(fileId).orElseThrow();
        String imageUrl = uploadedFile.getUrl();

        String markdownContent = String.format(
                "게시글 내용입니다.\n\n![이미지](%s)\n\n더 많은 내용...",
                imageUrl
        );

        assertThat(markdownContent).contains(imageUrl);
        assertThat(markdownContent).contains("![이미지]");
        assertThat(uploadedFile.getStatus()).isEqualTo(FileStatus.UPLOADED);
    }

    @Test
    @DisplayName("통합 테스트 - 여러 이미지 파일 업로드")
    void multipleImageUpload_success() throws Exception {
        PresignRequest request1 = new PresignRequest(
                FileType.IMAGE,
                "image1.jpg",
                "image/jpeg"
        );

        MvcResult result1 = mockMvc.perform(post("/api/v1/files/presign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated())
                .andReturn();

        PresignRequest request2 = new PresignRequest(
                FileType.IMAGE,
                "image2.png",
                "image/png"
        );

        MvcResult result2 = mockMvc.perform(post("/api/v1/files/presign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isCreated())
                .andReturn();

        Long fileId1 = objectMapper.readTree(result1.getResponse().getContentAsString())
                .get("data").get("fileId").asLong();
        Long fileId2 = objectMapper.readTree(result2.getResponse().getContentAsString())
                .get("data").get("fileId").asLong();

        assertThat(fileRepository.count()).isEqualTo(2);
        assertThat(fileId1).isNotEqualTo(fileId2);

        File file1 = fileRepository.findById(fileId1).orElseThrow();
        File file2 = fileRepository.findById(fileId2).orElseThrow();

        assertThat(file1.getOriginalName()).isEqualTo("image1.jpg");
        assertThat(file2.getOriginalName()).isEqualTo("image2.png");
        assertThat(file1.getStorageKey()).isNotEqualTo(file2.getStorageKey());
    }
}
