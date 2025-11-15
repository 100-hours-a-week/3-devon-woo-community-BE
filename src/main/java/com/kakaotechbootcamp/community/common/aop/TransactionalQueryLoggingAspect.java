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

import java.util.concurrent.atomic.AtomicLong;

@Aspect
@Component
@RequiredArgsConstructor
public class TransactionalQueryLoggingAspect {

    private static final Logger log = LoggerFactory.getLogger("community.aop.Transaction");
    private static final AtomicLong TX_ID_COUNTER = new AtomicLong(0);

    private final EntityManager entityManager;

    @Around("@annotation(org.springframework.transaction.annotation.Transactional)")
    public Object logTransactionQueryCount(ProceedingJoinPoint joinPoint) throws Throwable {
        long txId = TX_ID_COUNTER.incrementAndGet();
        String id = String.format("%03d", txId);

        Session session = entityManager.unwrap(Session.class);
        Statistics stats = session.getSessionFactory().getStatistics();
        stats.clear();

        long startQueryCount = stats.getPrepareStatementCount();
        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long executedQueries = stats.getPrepareStatementCount() - startQueryCount;
            long elapsedTimeMs = System.currentTimeMillis() - startTime;

            log.info("ID={} | Method={} | Queries={} | Time={}ms", id, joinPoint.getSignature().getName(), executedQueries, elapsedTimeMs);
            return result;
        } catch (Throwable throwable) {
            long executedQueries = stats.getPrepareStatementCount() - startQueryCount;
            long elapsedTimeMs = System.currentTimeMillis() - startTime;

            log.error("ID={} | Method={} | Queries={} | Time={}ms | Error={}", id, joinPoint.getSignature().getName(), executedQueries, elapsedTimeMs, throwable.getMessage());
            throw throwable;
        }
    }
}

