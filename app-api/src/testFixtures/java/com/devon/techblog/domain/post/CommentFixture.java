package com.devon.techblog.domain.post;

import com.devon.techblog.domain.member.entity.Member;
import com.devon.techblog.domain.post.entity.Comment;
import com.devon.techblog.domain.post.entity.Post;
import org.springframework.test.util.ReflectionTestUtils;

public final class CommentFixture {

    public static final String DEFAULT_CONTENT = "댓글내용";
    public static final String UPDATED_CONTENT = "수정된댓글";

    private CommentFixture() {}

    public static Comment create(Member member, Post post) {
        return Comment.create(member, post, DEFAULT_CONTENT);
    }

    public static Comment create(Member member, Post post, String content) {
        return Comment.create(member, post, content);
    }

    public static Comment createWithId(Long id, Member member, Post post) {
        Comment comment = create(member, post);
        ReflectionTestUtils.setField(comment, "id", id);
        return comment;
    }

    public static Comment createWithId(Long id, Member member, Post post, String content) {
        Comment comment = create(member, post, content);
        ReflectionTestUtils.setField(comment, "id", id);
        return comment;
    }
}
