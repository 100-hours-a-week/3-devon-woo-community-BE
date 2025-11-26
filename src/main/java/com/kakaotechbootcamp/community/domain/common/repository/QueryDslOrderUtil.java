package com.kakaotechbootcamp.community.domain.common.repository;

import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.ComparableExpressionBase;
import com.querydsl.core.types.dsl.EntityPathBase;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * QueryDSL 동적 정렬 유틸리티
 * Pageable의 Sort 정보를 QueryDSL OrderSpecifier로 변환
 */
public class QueryDslOrderUtil {

    /**
     * Pageable의 정렬 정보를 QueryDSL OrderSpecifier 배열로 변환
     * 화이트리스트 기반으로 허용된 필드만 정렬 가능
     *
     * @param pageable 페이징 정보
     * @param qClass Q-class 엔티티
     * @param allowedFields 허용된 필드명 (화이트리스트)
     * @return OrderSpecifier 배열
     */
    public static OrderSpecifier<?>[] getOrderSpecifiers(
            Pageable pageable,
            EntityPathBase<?> qClass,
            Set<String> allowedFields
    ) {
        List<OrderSpecifier<?>> orders = new ArrayList<>();

        if (pageable.getSort().isEmpty()) {
            return orders.toArray(new OrderSpecifier[0]);
        }

        for (Sort.Order sortOrder : pageable.getSort()) {
            String property = sortOrder.getProperty();

            // 화이트리스트 검증
            if (!allowedFields.contains(property)) {
                continue; // 허용되지 않은 필드는 무시
            }

            try {
                Path<?> path = getPath(qClass, property);
                if (path instanceof ComparableExpressionBase) {
                    Order direction = sortOrder.isAscending() ? Order.ASC : Order.DESC;
                    orders.add(new OrderSpecifier<>(direction, (ComparableExpressionBase<?>) path));
                }
            } catch (Exception e) {
                // 필드를 찾을 수 없는 경우 무시
                continue;
            }
        }

        return orders.toArray(new OrderSpecifier[0]);
    }

    /**
     * Map 기반 필드명 매핑을 지원하는 정렬 변환
     * API 필드명과 엔티티 필드명이 다를 때 사용
     *
     * @param pageable 페이징 정보
     * @param qClass Q-class 엔티티
     * @param fieldMapping API 필드명 -> 엔티티 필드명 매핑
     * @return OrderSpecifier 배열
     */
    public static OrderSpecifier<?>[] getOrderSpecifiers(
            Pageable pageable,
            EntityPathBase<?> qClass,
            Map<String, String> fieldMapping
    ) {
        List<OrderSpecifier<?>> orders = new ArrayList<>();

        if (pageable.getSort().isEmpty()) {
            return orders.toArray(new OrderSpecifier[0]);
        }

        for (Sort.Order sortOrder : pageable.getSort()) {
            String apiFieldName = sortOrder.getProperty();
            String entityFieldName = fieldMapping.get(apiFieldName);

            if (entityFieldName == null) {
                continue; // 매핑되지 않은 필드는 무시
            }

            try {
                Path<?> path = getPath(qClass, entityFieldName);
                if (path instanceof ComparableExpressionBase) {
                    Order direction = sortOrder.isAscending() ? Order.ASC : Order.DESC;
                    orders.add(new OrderSpecifier<>(direction, (ComparableExpressionBase<?>) path));
                }
            } catch (Exception e) {
                continue;
            }
        }

        return orders.toArray(new OrderSpecifier[0]);
    }

    /**
     * 리플렉션을 사용하여 Q-class의 필드 Path를 가져옴
     */
    private static Path<?> getPath(EntityPathBase<?> qClass, String fieldName) {
        try {
            return (Path<?>) qClass.getClass().getField(fieldName).get(qClass);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalArgumentException("Field not found: " + fieldName, e);
        }
    }

    /**
     * 기본 정렬 조건을 추가하는 헬퍼 메서드
     * 사용자 정렬이 없을 때 기본 정렬 적용
     */
    public static OrderSpecifier<?>[] getOrderSpecifiersWithDefault(
            Pageable pageable,
            EntityPathBase<?> qClass,
            Set<String> allowedFields,
            OrderSpecifier<?> defaultOrder
    ) {
        OrderSpecifier<?>[] orders = getOrderSpecifiers(pageable, qClass, allowedFields);

        if (orders.length == 0) {
            return new OrderSpecifier<?>[]{defaultOrder};
        }

        return orders;
    }
}
