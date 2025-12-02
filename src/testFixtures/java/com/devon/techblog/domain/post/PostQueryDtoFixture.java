package com.devon.techblog.domain.post;

import com.devon.techblog.domain.post.dto.PostSummaryQueryDto;
import java.time.Instant;

public final class PostQueryDtoFixture {

    public static final Long DEFAULT_POST_ID = 1L;
    public static final String DEFAULT_TITLE = "제목";
    public static final Long DEFAULT_VIEWS_COUNT = 0L;
    public static final Long DEFAULT_LIKE_COUNT = 0L;
    public static final Long DEFAULT_COMMENT_COUNT = 0L;
    public static final Long DEFAULT_MEMBER_ID = 1L;
    public static final String DEFAULT_MEMBER_NICKNAME = "tester";
    public static final String DEFAULT_SUMMARY = "요약";
    public static final String DEFAULT_THUMBNAIL = "https://example.com/thumbnail.jpg";

    private PostQueryDtoFixture() {}

    public static PostSummaryQueryDto create() {
        return new PostSummaryQueryDto(
                DEFAULT_POST_ID,
                DEFAULT_TITLE,
                Instant.now(),
                DEFAULT_VIEWS_COUNT,
                DEFAULT_LIKE_COUNT,
                DEFAULT_COMMENT_COUNT,
                DEFAULT_MEMBER_ID,
                DEFAULT_MEMBER_NICKNAME,
                null,
                DEFAULT_SUMMARY,
                DEFAULT_THUMBNAIL
        );
    }

    public static PostSummaryQueryDto create(Long postId, String title) {
        return new PostSummaryQueryDto(
                postId,
                title,
                Instant.now(),
                DEFAULT_VIEWS_COUNT,
                DEFAULT_LIKE_COUNT,
                DEFAULT_COMMENT_COUNT,
                DEFAULT_MEMBER_ID,
                DEFAULT_MEMBER_NICKNAME,
                null,
                DEFAULT_SUMMARY,
                DEFAULT_THUMBNAIL
        );
    }

    public static PostSummaryQueryDto createWithAllFields(
            Long postId,
            String title,
            Instant createdAt,
            Long viewsCount,
            Long likeCount,
            Long commentCount,
            Long memberId,
            String memberNickname,
            String memberProfileImageUrl,
            String summary,
            String thumbnail
    ) {
        return new PostSummaryQueryDto(
                postId,
                title,
                createdAt,
                viewsCount,
                likeCount,
                commentCount,
                memberId,
                memberNickname,
                memberProfileImageUrl,
                summary,
                thumbnail
        );
    }
}
