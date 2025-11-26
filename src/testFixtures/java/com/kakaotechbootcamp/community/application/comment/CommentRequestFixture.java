package com.kakaotechbootcamp.community.application.comment;

import com.kakaotechbootcamp.community.application.comment.dto.request.CommentCreateRequest;
import com.kakaotechbootcamp.community.application.comment.dto.request.CommentUpdateRequest;
import com.kakaotechbootcamp.community.domain.post.CommentFixture;

public final class CommentRequestFixture {

    private CommentRequestFixture() {}

    public static CommentCreateRequest createRequest() {
        return new CommentCreateRequest(CommentFixture.DEFAULT_CONTENT);
    }

    public static CommentCreateRequest createRequest(String content) {
        return new CommentCreateRequest(content);
    }

    public static CommentUpdateRequest updateRequest() {
        return new CommentUpdateRequest(CommentFixture.UPDATED_CONTENT);
    }

    public static CommentUpdateRequest updateRequest(String content) {
        return new CommentUpdateRequest(content);
    }
}
