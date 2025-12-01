package com.devon.techblog.domain.post.repository.impl;

import static com.devon.techblog.domain.member.entity.QMember.member;
import static com.devon.techblog.domain.post.entity.QPost.post;
import static com.devon.techblog.domain.post.entity.QPostTag.postTag;
import static com.devon.techblog.domain.post.entity.QTag.tag;

import com.devon.techblog.domain.common.repository.QueryDslOrderUtil;
import com.devon.techblog.domain.post.dto.PostSummaryQueryDto;
import com.devon.techblog.domain.post.repository.PostQueryRepository;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PostRepositoryImpl implements PostQueryRepository {

    private final JPAQueryFactory queryFactory;

    // 허용된 정렬 필드 (화이트리스트)
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id",
            "title",
            "viewsCount",
            "likeCount",
            "createdAt",
            "updatedAt"
    );

    @Override
    public Page<PostSummaryQueryDto> findAllActiveWithMemberAsDto(Pageable pageable) {
        List<PostSummaryQueryDto> content = queryFactory
                .select(Projections.constructor(PostSummaryQueryDto.class,
                        post.id,
                        post.title,
                        post.createdAt,
                        post.viewsCount,
                        post.likeCount,
                        post.commentCount,
                        member.id,
                        member.nickname,
                        member.profileImageUrl,
                        post.summary,
                        post.thumbnail
                ))
                .from(post)
                .join(post.member, member)
                .where(isNotDeleted())
                .orderBy(getOrderSpecifiers(pageable))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(post.count())
                .from(post)
                .where(isNotDeleted());

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    @Override
    public Page<PostSummaryQueryDto> searchByTitleOrContent(String keyword, Pageable pageable) {
        BooleanExpression condition = isNotDeleted().and(keywordSearch(keyword));

        List<PostSummaryQueryDto> content = queryFactory
                .select(Projections.constructor(PostSummaryQueryDto.class,
                        post.id,
                        post.title,
                        post.createdAt,
                        post.viewsCount,
                        post.likeCount,
                        post.commentCount,
                        member.id,
                        member.nickname,
                        member.profileImageUrl,
                        post.summary,
                        post.thumbnail
                ))
                .from(post)
                .join(post.member, member)
                .where(condition)
                .orderBy(getOrderSpecifiers(pageable))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(post.count())
                .from(post)
                .where(condition);

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    @Override
    public Page<PostSummaryQueryDto> findByTagsIn(List<String> tags, Pageable pageable) {
        if (tags == null || tags.isEmpty()) {
            return findAllActiveWithMemberAsDto(pageable);
        }

        BooleanExpression condition = isNotDeleted().and(hasAnyTag(tags));

        List<PostSummaryQueryDto> content = queryFactory
                .select(Projections.constructor(PostSummaryQueryDto.class,
                        post.id,
                        post.title,
                        post.createdAt,
                        post.viewsCount,
                        post.likeCount,
                        post.commentCount,
                        member.id,
                        member.nickname,
                        member.profileImageUrl,
                        post.summary,
                        post.thumbnail
                ))
                .from(post)
                .join(post.member, member)
                .where(condition)
                .orderBy(getOrderSpecifiers(pageable))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(post.count())
                .from(post)
                .where(condition);

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    private OrderSpecifier<?>[] getOrderSpecifiers(Pageable pageable) {
        return QueryDslOrderUtil.getOrderSpecifiersWithDefault(
                pageable,
                post,
                ALLOWED_SORT_FIELDS,
                post.createdAt.desc()
        );
    }

    private BooleanExpression isNotDeleted() {
        return post.isDeleted.eq(false);
    }

    private BooleanExpression keywordSearch(String keyword) {
        return titleContains(keyword).or(contentContains(keyword));
    }

    private BooleanExpression titleContains(String keyword) {
        return keyword != null ? post.title.containsIgnoreCase(keyword) : null;
    }

    private BooleanExpression contentContains(String keyword) {
        return keyword != null ? post.content.containsIgnoreCase(keyword) : null;
    }

    private BooleanExpression hasAnyTag(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return null;
        }

        return post.id.in(
            queryFactory
                .select(postTag.post.id)
                .from(postTag)
                .join(postTag.tag, tag)
                .where(tag.name.in(tags))
        );
    }
}
