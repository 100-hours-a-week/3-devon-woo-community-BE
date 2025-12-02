package com.devon.techblog.domain.post.entity;

import com.devon.techblog.domain.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "attachment")
public class File extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(name = "attachment_url", length = 500, nullable = false)
    private String attachmentUrl;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted;

    public static File create(Post post, String attachmentUrl) {
        Assert.notNull(post, "post required");
        Assert.hasText(attachmentUrl, "attachment url required");

        if (attachmentUrl.length() > 500) {
            throw new IllegalArgumentException("attachment url too long");
        }

        return File.builder()
                .post(post)
                .attachmentUrl(attachmentUrl)
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
