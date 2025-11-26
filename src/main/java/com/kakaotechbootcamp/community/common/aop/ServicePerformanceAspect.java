package com.kakaotechbootcamp.community.common.aop;

import java.util.concurrent.atomic.AtomicLong;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ServicePerformanceAspect {

    private static final Logger log = LoggerFactory.getLogger("community.aop.Service");
    private static final AtomicLong EXEC_ID_COUNTER = new AtomicLong(0);

    @Pointcut("execution(* com.kakaotechbootcamp.community.application.*.service.*.*(..))")
    public void serviceMethods() {}

    @Around("serviceMethods()")
    public Object measureExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long execId = EXEC_ID_COUNTER.incrementAndGet();
        String id = String.format("%03d", execId);

        String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;

            if (executionTime > 1000) {
                log.warn("ID={} | Method={}.{} | Time={}ms | Status=SLOW", id, className, methodName, executionTime);
            } else {
                log.info("ID={} | Method={}.{} | Time={}ms | Status=OK", id, className, methodName, executionTime);
            }

            return result;
        } catch (Throwable throwable) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("ID={} | Method={}.{} | Time={}ms | Status=ERROR | Message={}", id, className, methodName, executionTime, throwable.getMessage());
            throw throwable;
        }
    }
}
