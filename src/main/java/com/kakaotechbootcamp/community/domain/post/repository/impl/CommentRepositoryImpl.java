package com.kakaotechbootcamp.community.domain.post.repository.impl;

import static com.kakaotechbootcamp.community.domain.member.entity.QMember.member;
import static com.kakaotechbootcamp.community.domain.post.entity.QComment.comment;

import com.kakaotechbootcamp.community.domain.common.repository.QueryDslOrderUtil;
import com.kakaotechbootcamp.community.domain.post.dto.CommentQueryDto;
import com.kakaotechbootcamp.community.domain.post.repository.CommentQueryRepository;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CommentRepositoryImpl implements CommentQueryRepository {

    private final JPAQueryFactory queryFactory;

    // 허용된 정렬 필드 (화이트리스트)
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id",
            "content",
            "createdAt",
            "updatedAt"
    );

    @Override
    public Page<CommentQueryDto> findByPostIdWithMemberAsDto(Long postId, Pageable pageable) {
        OrderSpecifier<?>[] orders = QueryDslOrderUtil.getOrderSpecifiersWithDefault(
                pageable,
                comment,
                ALLOWED_SORT_FIELDS,
                comment.createdAt.asc()
        );

        List<CommentQueryDto> content = queryFactory
                .select(Projections.constructor(CommentQueryDto.class,
                        comment.id,
                        comment.post.id,
                        comment.content,
                        comment.createdAt,
                        comment.updatedAt,
                        member.id,
                        member.nickname,
                        member.profileImageUrl
                ))
                .from(comment)
                .join(comment.member, member)  // inner join (fetch join 아님)
                .where(comment.post.id.eq(postId))
                .orderBy(orders)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(comment.count())
                .from(comment)
                .where(comment.post.id.eq(postId));

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    @Override
    public Map<Long, Long> countCommentsByPostIds(List<Long> postIds) {
        List<Tuple> results = queryFactory
                .select(comment.post.id, comment.count())
                .from(comment)
                .where(comment.post.id.in(postIds))
                .groupBy(comment.post.id)
                .fetch();

        return results.stream()
                .collect(Collectors.toMap(
                        tuple -> tuple.get(comment.post.id),
                        tuple -> tuple.get(comment.count())
                ));
    }
}
