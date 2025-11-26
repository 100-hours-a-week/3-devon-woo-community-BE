package com.devon.techblog.infra.image.cloudinary;

import com.devon.techblog.infra.image.ImageSignature;
import com.devon.techblog.infra.image.ImageStorageService;
import java.util.Map;
import java.util.TreeMap;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CloudinaryImageStorageService implements ImageStorageService {

    @Value("${storage.cloudinary.api.key}")
    private String apiKey;

    @Value("${storage.cloudinary.api.secret}")
    private String apiSecret;

    @Value("${storage.cloudinary.cloud.name}")
    private String cloudName;

    @Value("${storage.cloudinary.upload.preset:unsigned_preset}")
    private String uploadPreset;

    @Override
    public ImageSignature generateUploadSignature(String type) {
        long timestamp = System.currentTimeMillis() / 1000L;

        String folder = "profile".equals(type) ? "profiles" : "posts";

        Map<String, String> paramsToSign = new TreeMap<>();
        paramsToSign.put("folder", folder);
        paramsToSign.put("timestamp", String.valueOf(timestamp));
        paramsToSign.put("upload_preset", uploadPreset);

        StringBuilder toSign = new StringBuilder();
        paramsToSign.forEach((key, value) -> {
            if (toSign.length() > 0) {
                toSign.append("&");
            }
            toSign.append(key).append("=").append(value);
        });
        toSign.append(apiSecret);

        String signature = DigestUtils.sha1Hex(toSign.toString());

        return new ImageSignature(
                apiKey,
                cloudName,
                timestamp,
                signature,
                uploadPreset,
                folder
        );
    }
}