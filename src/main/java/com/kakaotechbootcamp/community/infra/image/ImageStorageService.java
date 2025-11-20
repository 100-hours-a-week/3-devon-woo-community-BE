package com.kakaotechbootcamp.community.infra.image;

public interface ImageStorageService {
    ImageSignature generateUploadSignature(String type);
}