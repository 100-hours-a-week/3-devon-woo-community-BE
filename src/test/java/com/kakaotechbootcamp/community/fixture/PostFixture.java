package com.kakaotechbootcamp.community.fixture;

import com.kakaotechbootcamp.community.domain.member.entity.Member;
import com.kakaotechbootcamp.community.domain.post.entity.Post;
import org.springframework.test.util.ReflectionTestUtils;

public class PostFixture {

    public static final String DEFAULT_TITLE = "제목";
    public static final String DEFAULT_CONTENT = "내용";
    public static final String DEFAULT_IMAGE_URL = "https://example.com/image.jpg";
    public static final String UPDATED_TITLE = "수정된제목";
    public static final String UPDATED_CONTENT = "수정된내용";

    public static Post createPost(Member member) {
        return createPost(1L, member, DEFAULT_TITLE, DEFAULT_CONTENT);
    }

    public static Post createPost(Long id, Member member) {
        return createPost(id, member, DEFAULT_TITLE, DEFAULT_CONTENT);
    }

    public static Post createPost(Long id, Member member, String title, String content) {
        Post post = Post.create(member, title, content);
        ReflectionTestUtils.setField(post, "id", id);
        return post;
    }

    public static Post createPostWithTitle(Member member, String title) {
        return createPost(1L, member, title, DEFAULT_CONTENT);
    }

    public static Post createPostWithContent(Member member, String content) {
        return createPost(1L, member, DEFAULT_TITLE, content);
    }
}
