package com.kakaotechbootcamp.community.domain.common.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.JpaEntityInformationSupport;
import org.springframework.data.jpa.repository.support.Querydsl;
import org.springframework.data.querydsl.SimpleEntityPathResolver;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;
import org.springframework.util.Assert;

/**
 * QueryDSL 공통 지원 클래스
 * Repository Impl 클래스들이 상속받아 사용
 */
@Repository
public abstract class QueryDslSupport {

    private final Class<?> domainClass;
    private Querydsl querydsl;
    private EntityManager entityManager;
    private JPAQueryFactory queryFactory;

    public QueryDslSupport(Class<?> domainClass) {
        Assert.notNull(domainClass, "Domain class must not be null!");
        this.domainClass = domainClass;
    }

    @Autowired
    public void setEntityManager(EntityManager entityManager) {
        Assert.notNull(entityManager, "EntityManager must not be null!");
        JpaEntityInformation<?, ?> entityInformation =
                JpaEntityInformationSupport.getEntityInformation(domainClass, entityManager);
        SimpleEntityPathResolver resolver = SimpleEntityPathResolver.INSTANCE;
        this.querydsl = new Querydsl(entityManager,
                new com.querydsl.core.types.dsl.PathBuilder<>(entityInformation.getJavaType(),
                        entityInformation.getJavaType().getSimpleName()));
        this.entityManager = entityManager;
        this.queryFactory = new JPAQueryFactory(entityManager);
    }

    @PostConstruct
    public void validate() {
        Assert.notNull(entityManager, "EntityManager must not be null!");
        Assert.notNull(querydsl, "Querydsl must not be null!");
        Assert.notNull(queryFactory, "QueryFactory must not be null!");
    }

    protected JPAQueryFactory getQueryFactory() {
        return queryFactory;
    }

    protected Querydsl getQuerydsl() {
        return querydsl;
    }

    protected EntityManager getEntityManager() {
        return entityManager;
    }

    /**
     * 페이징 처리를 위한 헬퍼 메서드
     * content 쿼리와 count 쿼리를 분리하여 최적화
     */
    protected <T> Page<T> applyPagination(
            Pageable pageable,
            JPAQuery<T> contentQuery,
            JPAQuery<Long> countQuery
    ) {
        List<T> content = getQuerydsl().applyPagination(pageable, contentQuery).fetch();
        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    /**
     * 단순 페이징 처리 (count 쿼리가 content 쿼리와 동일한 경우)
     */
    protected <T> Page<T> applyPagination(
            Pageable pageable,
            JPAQuery<T> query
    ) {
        List<T> content = getQuerydsl().applyPagination(pageable, query).fetch();
        long total = query.fetchCount();
        return PageableExecutionUtils.getPage(content, pageable, () -> total);
    }

    /**
     * Null-safe BooleanExpression 생성
     */
    protected BooleanExpression nullSafeBuilder(BooleanExpression... expressions) {
        BooleanExpression result = null;
        for (BooleanExpression expression : expressions) {
            if (expression != null) {
                result = (result == null) ? expression : result.and(expression);
            }
        }
        return result;
    }

    /**
     * 빈 문자열을 null로 처리하는 헬퍼 메서드
     */
    protected String emptyToNull(String value) {
        return (value == null || value.trim().isEmpty()) ? null : value;
    }

    /**
     * COALESCE 표현식 생성 헬퍼
     * 사용 예: coalesce(post.likeCount, 0L)
     */
    protected <T extends Comparable<? super T>> com.querydsl.core.types.dsl.ComparableExpression<T> coalesce(
            com.querydsl.core.types.dsl.ComparableExpression<T> expression,
            T defaultValue
    ) {
        return expression.coalesce(defaultValue);
    }
}
