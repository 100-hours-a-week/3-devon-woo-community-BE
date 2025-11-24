package com.kakaotechbootcamp.community.fixture;

import com.kakaotechbootcamp.community.domain.member.entity.Member;
import com.kakaotechbootcamp.community.domain.post.entity.Comment;
import com.kakaotechbootcamp.community.domain.post.entity.Post;
import org.springframework.test.util.ReflectionTestUtils;

public class CommentFixture {

    public static final String DEFAULT_CONTENT = "댓글내용";
    public static final String UPDATED_CONTENT = "수정된댓글";

    public static Comment createComment(Member member, Post post) {
        return createComment(1L, member, post, DEFAULT_CONTENT);
    }

    public static Comment createComment(Long id, Member member, Post post) {
        return createComment(id, member, post, DEFAULT_CONTENT);
    }

    public static Comment createComment(Long id, Member member, Post post, String content) {
        Comment comment = Comment.create(member, post, content);
        ReflectionTestUtils.setField(comment, "id", id);
        return comment;
    }

    public static Comment createCommentWithContent(Member member, Post post, String content) {
        return createComment(1L, member, post, content);
    }
}
