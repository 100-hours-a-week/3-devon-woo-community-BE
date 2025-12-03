package com.devon.techblog.domain.file.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FileType {
    IMAGE("images"),
    VIDEO("videos"),
    DOCUMENT("documents");

    private final String folder;
}
