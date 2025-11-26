package com.devon.techblog.application.comment;

import com.devon.techblog.application.comment.dto.request.CommentCreateRequest;
import com.devon.techblog.application.comment.dto.request.CommentUpdateRequest;
import com.devon.techblog.domain.post.CommentFixture;

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
