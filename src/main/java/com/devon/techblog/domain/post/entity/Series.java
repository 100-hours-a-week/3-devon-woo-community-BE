package com.devon.techblog.domain.post.entity;

import com.devon.techblog.domain.common.entity.BaseTimeEntity;
import com.devon.techblog.domain.member.entity.Member;
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
@Table(name = "series")
public class Series extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "thumbnail", length = 500)
    private String thumbnail;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted;

    public static Series create(Member member, String name, String description) {
        validateCreate(member, name);
        return Series.builder()
                .member(member)
                .name(name)
                .description(description)
                .isDeleted(false)
                .build();
    }

    public void updateSeries(String name, String description, String thumbnail) {
        if (name != null) {
            Assert.hasText(name, "name required");
            if (name.length() > 100) {
                throw new IllegalArgumentException("name too long");
            }
            this.name = name;
        }
        if (description != null) {
            this.description = description;
        }
        if (thumbnail != null) {
            if (thumbnail.length() > 500) {
                throw new IllegalArgumentException("thumbnail url too long");
            }
            this.thumbnail = thumbnail;
        }
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

    private static void validateCreate(Member member, String name) {
        Assert.notNull(member, "member required");
        Assert.hasText(name, "name required");
        if (name.length() > 100) {
            throw new IllegalArgumentException("name too long");
        }
    }
}
