package com.devon.techblog.common.aop;

import com.devon.techblog.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

/**
 * 예외 로깅 및 알림 전용 Aspect.
 * <p>
 * - BusinessException: 예상 가능한 비즈니스 오류 → WARN 로그, 기본적으로 알림 전송 없음<br>
 * - 그 외 Exception: 시스템 오류(주로 500) → ERROR 로그 + 알림 전송 후보<br>
 * <p>
 * prod 환경에서는 스택 트레이스를 숨기고, dev/local 환경에서는 스택 트레이스를 포함한
 * 상세 메시지를 로그 및 알림에 포함한다.
 */
@Aspect
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "aop.exception-logging.enabled", havingValue = "true", matchIfMissing = true)
public class ExceptionLoggingAspect {

    private static final Logger log = LoggerFactory.getLogger("spring.aop.Exception");

    private final Environment environment;

    @AfterThrowing(
            pointcut = "execution(* com.spring.mvc.base.application.*.controller.*.*(..))",
            throwing = "ex"
    )
    public void logAndNotifyException(JoinPoint joinPoint, Throwable ex) {
        boolean isProd = environment.acceptsProfiles(Profiles.of("prod"));
        boolean includeStackTrace = !isProd;

        if (ex instanceof BusinessException businessException) {
            logBusinessException(joinPoint, businessException, includeStackTrace);
            return;
        }

        logSystemException(joinPoint, ex, includeStackTrace);

        // 시스템 예외는 빠른 인지를 위해 알림 연동을 고려한다.
        notifyOnSystemException(ex, includeStackTrace);
    }

    private void logBusinessException(JoinPoint joinPoint, BusinessException ex, boolean includeStackTrace) {
        String signature = joinPoint.getSignature().toShortString();

        if (includeStackTrace) {
            log.warn("BusinessException at {} | Code={} | Message={}",
                    signature, ex.getErrorCode(), ex.getMessage(), ex);
        } else {
            log.warn("BusinessException at {} | Code={} | Message={}",
                    signature, ex.getErrorCode(), ex.getMessage());
        }
    }

    private void logSystemException(JoinPoint joinPoint, Throwable ex, boolean includeStackTrace) {
        String signature = joinPoint.getSignature().toShortString();

        if (includeStackTrace) {
            log.error("Unhandled exception at {} | Message={}", signature, ex.getMessage(), ex);
        } else {
            log.error("Unhandled exception at {} | Message={}", signature, ex.getMessage());
        }
    }

    /**
     * 시스템 예외 발생시 외부 알림(예: Slack, Webhook, Email)을 보낼 수 있는 확장 지점.
     * <p>
     * prod: 스택 트레이스를 제외한 요약 정보만 전송<br>
     * dev/local: 스택 트레이스를 포함한 상세 정보를 전송
     */
    private void notifyOnSystemException(Throwable ex, boolean includeStackTrace) {
        // TODO: 실제 Webhook/Slack/Email 연동 구현
        if (includeStackTrace) {
            log.debug("[ALERT] Sending detailed system exception notification with stack trace: {}", ex.getMessage(), ex);
        } else {
            log.debug("[ALERT] Sending summarized system exception notification: {}", ex.getMessage());
        }
    }
}

