package com.kakaotechbootcamp.community.common.aop;


import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.hibernate.Session;
import org.hibernate.stat.Statistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import org.springframework.transaction.annotation.Transactional;

@Aspect
@Component
@RequiredArgsConstructor
public class TransactionQueryCountAspect {

    private static final Logger log = LoggerFactory.getLogger("community.aop.TransactionQueryCountAspect");

    private final EntityManager entityManager;

    @Around("@annotation(org.springframework.transaction.annotation.Transactional)")
    public Object logTransactionQueryCount(ProceedingJoinPoint joinPoint) throws Throwable {
        Session session = entityManager.unwrap(Session.class);
        Statistics stats = session.getSessionFactory().getStatistics();

        stats.clear();

        long startQueryCount = stats.getPrepareStatementCount();
        long startTime = System.currentTimeMillis();

        Object result = joinPoint.proceed();

        long endQueryCount = stats.getPrepareStatementCount();
        long executedQueries = endQueryCount - startQueryCount;
        long elapsedTimeMs = System.currentTimeMillis() - startTime;

        log.info("[Transaction Query Log] Method: " + joinPoint.getSignature().getName()
                + ", Executed Queries: " + executedQueries
                + ", Time: " + elapsedTimeMs + "ms");

        return result;
    }
}

