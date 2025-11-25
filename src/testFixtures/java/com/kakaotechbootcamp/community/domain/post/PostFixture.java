package com.kakaotechbootcamp.community.domain.post;

import com.kakaotechbootcamp.community.domain.member.entity.Member;
import com.kakaotechbootcamp.community.domain.post.entity.Post;
import org.springframework.test.util.ReflectionTestUtils;

public final class PostFixture {

    public static final String DEFAULT_TITLE = "제목";
    public static final String DEFAULT_CONTENT = "내용";
    public static final String DEFAULT_IMAGE_URL = "https://example.com/image.jpg";
    public static final String UPDATED_TITLE = "수정된제목";
    public static final String UPDATED_CONTENT = "수정된내용";

    private PostFixture() {}

    public static Post create(Member member) {
        return Post.create(member, DEFAULT_TITLE, DEFAULT_CONTENT);
    }

    public static Post create(Member member, String title, String content) {
        return Post.create(member, title, content);
    }

    public static Post createWithId(Long id, Member member) {
        Post post = create(member);
        ReflectionTestUtils.setField(post, "id", id);
        return post;
    }

    public static Post createWithId(Long id, Member member, String title, String content) {
        Post post = create(member, title, content);
        ReflectionTestUtils.setField(post, "id", id);
        return post;
    }
}
