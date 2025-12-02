package com.devon.techblog.domain.common.entity;

import com.devon.techblog.domain.common.enums.FileType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.Assert;

@Entity
@Getter
@Builder(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "file")
public class File extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false, length = 20)
    private FileType fileType;

    @Column(name = "original_name", length = 255, nullable = false)
    private String originalName;

    @Column(name = "s3_key", length = 500, nullable = false)
    private String s3Key;

    @Column(name = "url", length = 500, nullable = false)
    private String url;

    @Column(name = "size", nullable = false)
    private Long size;

    @Column(name = "mime_type", length = 100, nullable = false)
    private String mimeType;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @Column(name = "duration")
    private Integer duration;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted;

    public static File create(
            FileType fileType,
            String originalName,
            String s3Key,
            String url,
            Long size,
            String mimeType,
            Integer width,
            Integer height,
            Integer duration
    ) {
        Assert.notNull(fileType, "file type required");
        Assert.hasText(originalName, "original name required");
        Assert.hasText(s3Key, "s3 key required");
        Assert.hasText(url, "url required");
        Assert.notNull(size, "size required");
        Assert.hasText(mimeType, "mime type required");

        if (size <= 0) {
            throw new IllegalArgumentException("size must be positive");
        }

        if (originalName.length() > 255) {
            throw new IllegalArgumentException("original name too long");
        }

        if (s3Key.length() > 500) {
            throw new IllegalArgumentException("s3 key too long");
        }

        if (url.length() > 500) {
            throw new IllegalArgumentException("url too long");
        }

        if (mimeType.length() > 100) {
            throw new IllegalArgumentException("mime type too long");
        }

        return File.builder()
                .fileType(fileType)
                .originalName(originalName)
                .s3Key(s3Key)
                .url(url)
                .size(size)
                .mimeType(mimeType)
                .width(width)
                .height(height)
                .duration(duration)
                .isDeleted(false)
                .build();
    }

    public void delete() {
        this.isDeleted = true;
    }

    public void restore() {
        this.isDeleted = false;
    }

    public boolean isDeleted() {
        return this.isDeleted;
    }
}
