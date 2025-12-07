package com.devon.techblog.infra.image;

import com.devon.techblog.common.exception.BusinessException;
import com.devon.techblog.common.exception.code.FileErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "storage.cloudinary", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoOpImageStorageService implements ImageStorageService {

    private static final Logger log = LoggerFactory.getLogger(NoOpImageStorageService.class);

    @Override
    public ImageSignature generateUploadSignature(String type) {
        log.warn("CloudinaryConfig 또는 ImageStorageService 구현이 없어 파일 업로드 기능이 비활성화되었습니다.");
        throw new BusinessException(FileErrorCode.FILE_STORAGE_NOT_CONFIGURED);
    }
}

