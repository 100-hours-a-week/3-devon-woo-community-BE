package com.devon.techblog.domain.file.util;

import com.devon.techblog.domain.file.entity.FileType;
import java.util.UUID;

public class FileStorageKeyGenerator {

    private FileStorageKeyGenerator() {
    }

    public static String generate(FileType fileType, String originalName) {
        String uuid = UUID.randomUUID().toString();
        String extension = extractExtension(originalName);
        return fileType.getFolder() + "/" + uuid + extension;
    }

    public static String extractExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0 && lastDot < filename.length() - 1) {
            return filename.substring(lastDot);
        }
        return "";
    }
}
