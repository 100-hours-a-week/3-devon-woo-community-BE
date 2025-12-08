package com.devon.techblog.common.aop;

import jakarta.servlet.http.HttpServletRequest;
import java.util.concurrent.atomic.AtomicLong;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * API 호출에 대한 공통 Request/Response 로깅 Aspect.
 * <p>
 * 예외에 대한 상세 로깅 및 알림은 {@code ExceptionLoggingAspect}에서 처리한다.
 */
@Aspect
@Component
@ConditionalOnProperty(name = "aop.api-logging.enabled", havingValue = "true", matchIfMissing = true)
public class ApiLoggingAspect {

    private static final Logger log = LoggerFactory.getLogger("spring.aop.API");

    private static final AtomicLong REQUEST_ID_COUNTER = new AtomicLong(0);

    @Pointcut("execution(* com.devon.techblog.application.*.controller.*.*(..))")
    public void controllerMethods() {}

    @Around("controllerMethods()")
    public Object logApiCall(ProceedingJoinPoint joinPoint) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes == null) {
            return joinPoint.proceed();
        }

        HttpServletRequest request = attributes.getRequest();
        long requestId = REQUEST_ID_COUNTER.incrementAndGet();
        String id = String.format("%03d", requestId); // 3자리 ID
        long startTime = System.currentTimeMillis();

        // Request 로그
        log.info("ID={} | Endpoint={} {} | IP={}",
                id,
                request.getMethod(),
                request.getRequestURI(),
                request.getRemoteAddr());

        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;

            // Response 로그 (정상 처리)
            log.info("ID={} | Time={}ms", id, duration);

            return result;
        } finally {
            // 예외가 발생해도 실행 시간 측정은 유지된다.
        }
    }
}
