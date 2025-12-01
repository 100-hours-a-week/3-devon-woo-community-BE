package com.devon.techblog.domain.post.entity;

import com.devon.techblog.domain.common.entity.BaseTimeEntity;
import com.devon.techblog.domain.member.entity.Member;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
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
@Table(name = "post")
public class Post extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "title", length = 200, nullable = false)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "views_count", nullable = false)
    private Long viewsCount;

    @Column(name = "like_count", nullable = false)
    private Long likeCount;

    @Column(name = "comment_count", nullable = false)
    private Long commentCount;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted;

    @Column(name = "summary", length = 500)
    private String summary;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PostTag> postTags = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "series_id")
    private Series series;

    @Column(name = "visibility", length = 20)
    private String visibility;

    @Column(name = "is_draft", nullable = false)
    private Boolean isDraft;

    @Column(name = "comments_allowed", nullable = false)
    private Boolean commentsAllowed;

    @Column(name = "thumbnail", length = 500)
    private String thumbnail;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    public static Post create(Member member, String title, String content) {
        validateCreate(member, title, content);
        return Post.builder()
                .member(member)
                .title(title)
                .content(content)
                .viewsCount(0L)
                .likeCount(0L)
                .commentCount(0L)
                .isDeleted(false)
                .visibility("public")
                .isDraft(false)
                .commentsAllowed(true)
                .build();
    }

    public void updatePost(String title, String content) {
        if (title != null) {
            Assert.hasText(title, "title required");
            if (title.length() > 200) {
                throw new IllegalArgumentException("title too long");
            }
            this.title = title;
        }
        if (content != null) {
            Assert.hasText(content, "content required");
            this.content = content;
        }
    }

    public void incrementViews() {
        this.viewsCount++;
    }

    public void incrementLikes() {
        this.likeCount++;
    }

    public void decrementLikes() {
        if (this.likeCount > 0) {
            this.likeCount--;
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

    public void updateSummary(String summary) {
        if (summary != null && summary.length() > 500) {
            throw new IllegalArgumentException("summary too long");
        }
        this.summary = summary;
    }

    public void addPostTag(PostTag postTag) {
        Assert.notNull(postTag, "postTag required");
        this.postTags.add(postTag);
    }

    public void clearPostTags() {
        this.postTags.clear();
    }

    public List<String> getTagNames() {
        return this.postTags.stream()
                .map(pt -> pt.getTag().getName())
                .toList();
    }

    public void setSeries(Series series) {
        this.series = series;
    }

    public void removeSeries() {
        this.series = null;
    }

    public void updateVisibility(String visibility) {
        if (visibility != null && visibility.length() > 20) {
            throw new IllegalArgumentException("visibility too long");
        }
        this.visibility = visibility;
    }

    public void markAsDraft() {
        this.isDraft = true;
    }

    public void publish() {
        this.isDraft = false;
    }

    public void setCommentsAllowed(Boolean commentsAllowed) {
        this.commentsAllowed = commentsAllowed != null ? commentsAllowed : true;
    }

    public void updateThumbnail(String thumbnail) {
        if (thumbnail != null && thumbnail.length() > 500) {
            throw new IllegalArgumentException("thumbnail url too long");
        }
        this.thumbnail = thumbnail;
    }

    public void updateImageUrl(String imageUrl) {
        if (imageUrl != null && imageUrl.length() > 500) {
            throw new IllegalArgumentException("image url too long");
        }
        this.imageUrl = imageUrl;
    }

    private static void validateCreate(Member member, String title, String content){
        Assert.notNull(member, "member required");
        Assert.hasText(title, "title required");
        Assert.hasText(content, "content required");

        if (title.length() > 200) {
            throw new IllegalArgumentException("title too long");
        }
    }
}
