package com.devon.techblog.domain.post.entity;

import com.devon.techblog.domain.common.entity.CreatedOnlyEntity;
import com.devon.techblog.domain.member.entity.Member;
import com.devon.techblog.domain.post.entity.id.PostLikeId;
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
import org.springframework.data.domain.Persistable;
import org.springframework.util.Assert;

@Entity
@Getter
@Builder(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "post_like")
public class PostLike extends CreatedOnlyEntity implements Persistable<PostLikeId> {

    @EmbeddedId
    private PostLikeId id;

    @MapsId("postId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Post post;

    @MapsId("memberId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Member member;

    public static PostLike create(Post post, Member member) {
        validateCreate(post, member);
        return PostLike.builder()
                .id(PostLikeId.create(post.getId(), member.getId()))
                .post(post)
                .member(member)
                .build();
    }

    private static void validateCreate(Post post, Member member) {
        Assert.notNull(post, "post required");
        Assert.notNull(member, "member required");
    }

    @Override
    public boolean isNew() {
        return getCreatedAt() == null;
    }
}
