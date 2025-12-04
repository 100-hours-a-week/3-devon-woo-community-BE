package com.devon.techblog.domain.post.entity;

import com.devon.techblog.domain.post.entity.id.PostTagId;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.util.Assert;

@Entity
@Getter
@Builder(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "post_tag")
public class PostTag {

    @EmbeddedId
    private PostTagId id;

    @MapsId("postId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Post post;

    @MapsId("tagId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    public static PostTag create(Post post, Tag tag) {
        validateCreate(post, tag);
        return PostTag.builder()
                .id(PostTagId.create(post.getId(), tag.getId()))
                .post(post)
                .tag(tag)
                .build();
    }

    private static void validateCreate(Post post, Tag tag) {
        Assert.notNull(post, "post required");
        Assert.notNull(tag, "tag required");
    }
}
