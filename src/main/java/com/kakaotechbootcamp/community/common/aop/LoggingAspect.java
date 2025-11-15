package com.kakaotechbootcamp.community.common.aop;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.atomic.AtomicLong;

@Aspect
@Component
public class LoggingAspect {

    private static final Logger log = LoggerFactory.getLogger("community.aop.LoggingAspect");

    private static final AtomicLong REQUEST_ID_COUNTER = new AtomicLong(0);

    @Pointcut("execution(* com.kakaotechbootcamp.community.application.*.controller.*.*(..))")
    public void controllerMethods() {}

    @Around("controllerMethods()")
    public Object logApiCall(ProceedingJoinPoint joinPoint) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes == null) {
            return joinPoint.proceed();
        }

        HttpServletRequest request = attributes.getRequest();
        long requestId = REQUEST_ID_COUNTER.incrementAndGet();
        long startTime = System.currentTimeMillis();

        // Request 로그
        log.info("[REQ-{}] {} {} | IP: {}",
                requestId,
                request.getMethod(),
                request.getRequestURI(),
                request.getRemoteAddr());

        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;

            // Response 로그 (성공)
            log.info("[RES-{}] {} | {}ms",
                    requestId,
                    result != null ? result.getClass().getSimpleName() : "void",
                    duration);

            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;

            // Response 로그 (실패)
            log.error("[ERR-{}] {} | {}ms | {}",
                    requestId,
                    e.getClass().getSimpleName(),
                    duration,
                    e.getMessage());

            throw e;
        }
    }
}
