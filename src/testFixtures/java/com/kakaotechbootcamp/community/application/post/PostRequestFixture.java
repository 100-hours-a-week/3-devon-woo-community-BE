package com.kakaotechbootcamp.community.application.post;

import com.kakaotechbootcamp.community.application.post.dto.request.PostCreateRequest;
import com.kakaotechbootcamp.community.application.post.dto.request.PostUpdateRequest;
import com.kakaotechbootcamp.community.domain.post.PostFixture;

public final class PostRequestFixture {

    private PostRequestFixture() {}

    public static PostCreateRequest createRequest() {
        return new PostCreateRequest(PostFixture.DEFAULT_TITLE, PostFixture.DEFAULT_CONTENT, null);
    }

    public static PostCreateRequest createRequest(String title, String content) {
        return new PostCreateRequest(title, content, null);
    }

    public static PostCreateRequest createRequest(String title, String content, String imageUrl) {
        return new PostCreateRequest(title, content, imageUrl);
    }

    public static PostUpdateRequest updateRequest() {
        return new PostUpdateRequest(PostFixture.UPDATED_TITLE, PostFixture.UPDATED_CONTENT, null);
    }

    public static PostUpdateRequest updateRequest(String title, String content) {
        return new PostUpdateRequest(title, content, null);
    }

    public static PostUpdateRequest updateRequest(String title, String content, String imageUrl) {
        return new PostUpdateRequest(title, content, imageUrl);
    }
}
