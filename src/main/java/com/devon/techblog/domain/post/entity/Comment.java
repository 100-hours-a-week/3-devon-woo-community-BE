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
@Table(name = "comment")
public class Comment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted;

    public static Comment create(Member member, Post post, String content) {
        Assert.notNull(member, "member required");
        Assert.notNull(post, "post required");
        Assert.hasText(content, "content required");

        return Comment.builder()
                .member(member)
                .post(post)
                .content(content)
                .isDeleted(false)
                .build();
    }

    public void updateContent(String content) {
        if (content != null) {
            Assert.hasText(content, "content required");
            this.content = content;
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
}
