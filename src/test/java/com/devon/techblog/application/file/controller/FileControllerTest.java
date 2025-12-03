package com.devon.techblog.application.file.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.devon.techblog.application.file.FileRequestFixture;
import com.devon.techblog.application.file.dto.request.FileCreateRequest;
import com.devon.techblog.common.exception.CustomException;
import com.devon.techblog.common.exception.code.FileErrorCode;
import com.devon.techblog.config.annotation.ControllerWebMvcTest;
import com.devon.techblog.domain.file.FileFixture;
import com.devon.techblog.domain.file.entity.File;
import com.devon.techblog.domain.file.entity.FileType;
import com.devon.techblog.domain.file.service.FileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@ControllerWebMvcTest(FileController.class)
class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FileService fileService;

    @Test
    @DisplayName("파일 메타데이터 생성 - 201 Created")
    void createFile_success() throws Exception {
        FileCreateRequest request = FileRequestFixture.createRequest();
        File file = FileFixture.createWithId(1L);

        given(fileService.createFile(any(), any(), any(), any(), any(), any()))
                .willReturn(file);

        mockMvc.perform(post("/api/v1/files")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.fileType").value("IMAGE"))
                .andExpect(jsonPath("$.data.originalName").value(FileFixture.DEFAULT_ORIGINAL_NAME))
                .andExpect(jsonPath("$.data.storageKey").value(FileFixture.DEFAULT_STORAGE_KEY))
                .andExpect(jsonPath("$.data.url").value(FileFixture.DEFAULT_URL))
                .andExpect(jsonPath("$.data.size").value(FileFixture.DEFAULT_SIZE))
                .andExpect(jsonPath("$.data.mimeType").value(FileFixture.DEFAULT_MIME_TYPE))
                .andExpect(jsonPath("$.data.isDeleted").value(false));
    }

    @Test
    @DisplayName("파일 생성 시 필수값 누락 - 400 Bad Request")
    void createFile_missingRequiredFields_badRequest() throws Exception {
        FileCreateRequest invalidRequest = new FileCreateRequest(
                null, "", "", "", null, ""
        );

        mockMvc.perform(post("/api/v1/files")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("ID로 파일 조회 - 200 OK")
    void getFile_success() throws Exception {
        File file = FileFixture.createWithId(1L);
        given(fileService.getFileById(1L)).willReturn(file);

        mockMvc.perform(get("/api/v1/files/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.originalName").value(FileFixture.DEFAULT_ORIGINAL_NAME))
                .andExpect(jsonPath("$.data.storageKey").value(FileFixture.DEFAULT_STORAGE_KEY));
    }

    @Test
    @DisplayName("존재하지 않는 파일 조회 - 예외 발생")
    void getFile_notFound_throwsException() throws Exception {
        given(fileService.getFileById(999L))
                .willThrow(new CustomException(FileErrorCode.FILE_NOT_FOUND));

        mockMvc.perform(get("/api/v1/files/{id}", 999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("storageKey로 파일 조회 - 200 OK")
    void getFileByStorageKey_success() throws Exception {
        File file = FileFixture.createWithId(1L);
        given(fileService.getFileByStorageKey(FileFixture.DEFAULT_STORAGE_KEY)).willReturn(file);

        mockMvc.perform(get("/api/v1/files/by-key")
                        .param("storageKey", FileFixture.DEFAULT_STORAGE_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.storageKey").value(FileFixture.DEFAULT_STORAGE_KEY));
    }

    @Test
    @DisplayName("전체 파일 목록 조회 - 200 OK")
    void getAllFiles_success() throws Exception {
        File file1 = FileFixture.createWithId(1L);
        File file2 = FileFixture.createWithId(2L);
        given(fileService.getAllFiles()).willReturn(List.of(file1, file2));

        mockMvc.perform(get("/api/v1/files"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value(1L))
                .andExpect(jsonPath("$.data[1].id").value(2L));
    }

    @Test
    @DisplayName("파일 타입으로 필터링하여 조회 - 200 OK")
    void getFilesByType_success() throws Exception {
        File file1 = FileFixture.createWithId(1L);
        File file2 = FileFixture.createWithId(2L);
        given(fileService.getFilesByType(FileType.IMAGE)).willReturn(List.of(file1, file2));

        mockMvc.perform(get("/api/v1/files")
                        .param("fileType", "IMAGE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].fileType").value("IMAGE"))
                .andExpect(jsonPath("$.data[1].fileType").value("IMAGE"));
    }

    @Test
    @DisplayName("파일 삭제 - 204 No Content")
    void deleteFile_success() throws Exception {
        mockMvc.perform(delete("/api/v1/files/{id}", 1L))
                .andExpect(status().isNoContent());

        verify(fileService).deleteFile(1L);
    }

    @Test
    @DisplayName("파일 복구 - 204 No Content")
    void restoreFile_success() throws Exception {
        mockMvc.perform(patch("/api/v1/files/{id}/restore", 1L))
                .andExpect(status().isNoContent());

        verify(fileService).restoreFile(1L);
    }

    @Test
    @DisplayName("파일 영구 삭제 - 204 No Content")
    void permanentlyDeleteFile_success() throws Exception {
        mockMvc.perform(delete("/api/v1/files/{id}/permanent", 1L))
                .andExpect(status().isNoContent());

        verify(fileService).permanentlyDeleteFile(1L);
    }

    @Test
    @DisplayName("존재하지 않는 파일 삭제 시도 - 예외 발생")
    void deleteFile_notFound_throwsException() throws Exception {
        willThrow(new CustomException(FileErrorCode.FILE_NOT_FOUND))
                .given(fileService).deleteFile(999L);

        mockMvc.perform(delete("/api/v1/files/{id}", 999L))
                .andExpect(status().isNotFound());
    }
}
