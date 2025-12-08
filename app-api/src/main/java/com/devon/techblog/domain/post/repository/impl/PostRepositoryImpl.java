package com.devon.techblog.domain.post.repository.impl;

import static com.devon.techblog.domain.member.entity.QMember.member;
import static com.devon.techblog.domain.post.entity.QPost.post;
import static com.devon.techblog.domain.post.entity.QPostTag.postTag;
import static com.devon.techblog.domain.post.entity.QTag.tag;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.devon.techblog.domain.common.repository.QueryDslOrderUtil;
import com.devon.techblog.domain.post.dto.PostSearchCondition;
import com.devon.techblog.domain.post.dto.PostSummaryQueryDto;
import com.devon.techblog.domain.post.repository.PostQueryRepository;
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
    public Page<PostSummaryQueryDto> searchPosts(PostSearchCondition condition, Pageable pageable) {
        PostSearchCondition effectiveCondition = condition != null ? condition : PostSearchCondition.empty();
        BooleanBuilder predicate = new BooleanBuilder();
        predicate.and(isNotDeleted());
        addCondition(predicate, containsKeyword(effectiveCondition.keyword()));
        addCondition(predicate, eqMemberId(effectiveCondition.memberId()));
        addCondition(predicate, inTags(effectiveCondition.tags()));

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
                .where(predicate)
                .orderBy(getOrderSpecifiers(pageable))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        var countQuery = queryFactory
                .select(post.count())
                .from(post)
                .where(predicate);

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

    private BooleanExpression containsKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }

        return post.title.containsIgnoreCase(keyword)
                .or(post.content.containsIgnoreCase(keyword));
    }

    private BooleanExpression eqMemberId(Long memberId) {
        return memberId != null ? post.member.id.eq(memberId) : null;
    }

    private BooleanExpression inTags(List<String> tags) {
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

    private void addCondition(BooleanBuilder builder, BooleanExpression expression) {
        if (expression != null) {
            builder.and(expression);
        }
    }
}
