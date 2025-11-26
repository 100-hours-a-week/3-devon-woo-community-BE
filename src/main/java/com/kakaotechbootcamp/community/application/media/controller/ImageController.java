package com.kakaotechbootcamp.community.application.media.controller;

import com.kakaotechbootcamp.community.common.dto.api.ApiResponse;
import com.kakaotechbootcamp.community.infra.image.ImageSignature;
import com.kakaotechbootcamp.community.infra.image.ImageStorageService;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageStorageService imageStorageService;

    @GetMapping("/sign")
    public ApiResponse<Map<String, Object>> sign(
            @RequestParam(value = "type", defaultValue = "post") String type
    ) {
        ImageSignature signature = imageStorageService.generateUploadSignature(type);

        Map<String, Object> body = new HashMap<>();
        body.put("apiKey", signature.apiKey());
        body.put("cloudName", signature.cloudName());
        body.put("timestamp", signature.timestamp());
        body.put("signature", signature.signature());
        body.put("uploadPreset", signature.uploadPreset());
        body.put("folder", signature.folder());

        return ApiResponse.success(body);
    }
}