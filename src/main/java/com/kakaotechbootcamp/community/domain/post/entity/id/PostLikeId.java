package com.kakaotechbootcamp.community.domain.post.entity.id;

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
public class PostLikeId implements Serializable {

    private Long postId;
    private Long memberId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PostLikeId)) return false;

        PostLikeId that = (PostLikeId) o;
        return Objects.equals(postId, that.postId) &&
                Objects.equals(memberId, that.memberId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(postId, memberId);
    }

    public static PostLikeId create(Long postId, Long memberId) {
        validateCreate(postId, memberId);
        return PostLikeId.builder()
                .postId(postId)
                .memberId(memberId)
                .build();
    }

    private static void validateCreate(Long postId, Long memberId) {
        if (postId == null) {
            throw new IllegalArgumentException("postId required");
        }
        if (memberId == null) {
            throw new IllegalArgumentException("memberId required");
        }
    }
}
