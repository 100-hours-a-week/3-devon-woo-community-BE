package com.devon.techblog.application.post.dto;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Builder;
import lombok.Getter;

/**
 * 조회 컨텍스트 정보
 * 조회수 증가 정책 판단에 필요한 정보를 담는다.
 */
@Getter
@Builder
public class ViewContext {

    private final Long memberId;      // 로그인 사용자 ID (nullable)
    private final String ipAddress;   // 클라이언트 IP
    private final String userAgent;   // User-Agent

    /**
     * HttpServletRequest로부터 ViewContext 생성
     */
    public static ViewContext from(HttpServletRequest request, Long memberId) {
        return ViewContext.builder()
                .memberId(memberId)
                .ipAddress(extractIpAddress(request))
                .userAgent(request.getHeader("User-Agent"))
                .build();
    }

    /**
     * IP 주소 추출 (Proxy, Load Balancer 고려)
     */
    private static String extractIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // X-Forwarded-For는 콤마로 구분된 여러 IP가 올 수 있음 (첫 번째가 원본 클라이언트)
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }
}
