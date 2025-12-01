package com.devon.techblog.domain.post.entity.id;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@Builder(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class PostTagId implements Serializable {

    private Long postId;
    private Long tagId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PostTagId)) return false;

        PostTagId that = (PostTagId) o;
        return Objects.equals(postId, that.postId) &&
                Objects.equals(tagId, that.tagId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(postId, tagId);
    }

    public static PostTagId create(Long postId, Long tagId) {
        validateCreate(postId, tagId);
        return PostTagId.builder()
                .postId(postId)
                .tagId(tagId)
                .build();
    }

    private static void validateCreate(Long postId, Long tagId) {
        if (postId == null) {
            throw new IllegalArgumentException("postId required");
        }
        if (tagId == null) {
            throw new IllegalArgumentException("tagId required");
        }
    }
}
