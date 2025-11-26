package com.kakaotechbootcamp.community.domain.post.repository.impl;

import static com.kakaotechbootcamp.community.domain.member.entity.QMember.member;
import static com.kakaotechbootcamp.community.domain.post.entity.QPost.post;

import com.kakaotechbootcamp.community.domain.common.repository.QueryDslOrderUtil;
import com.kakaotechbootcamp.community.domain.post.dto.PostQueryDto;
import com.kakaotechbootcamp.community.domain.post.repository.PostQueryRepository;
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
    public Page<PostQueryDto> findAllActiveWithMemberAsDto(Pageable pageable) {
        List<PostQueryDto> content = queryFactory
                .select(Projections.constructor(PostQueryDto.class,
                        post.id,
                        post.title,
                        post.createdAt,
                        post.viewsCount,
                        post.likeCount,
                        post.commentCount,
                        member.id,
                        member.nickname,
                        member.profileImageUrl
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
    public Page<PostQueryDto> searchByTitleOrContent(String keyword, Pageable pageable) {
        BooleanExpression condition = isNotDeleted().and(keywordSearch(keyword));

        List<PostQueryDto> content = queryFactory
                .select(Projections.constructor(PostQueryDto.class,
                        post.id,
                        post.title,
                        post.createdAt,
                        post.viewsCount,
                        post.likeCount,
                        post.commentCount,
                        member.id,
                        member.nickname,
                        member.email
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
}
