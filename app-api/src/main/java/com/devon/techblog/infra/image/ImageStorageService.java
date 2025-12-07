package com.devon.techblog.infra.image;

public interface ImageStorageService {

    ImageSignature generateUploadSignature(String folder);
}
