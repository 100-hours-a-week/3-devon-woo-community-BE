package com.kakaotechbootcamp.community.application.security.config;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * CORS 설정을 외부 설정 파일(application.yml)에서 주입받는 Properties 클래스
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "spring.security.cors")
public class CorsProperties {

    /**
     * 허용할 Origin 목록
     * 예: ["http://localhost:3000", "https://example.com"]
     */
    private List<String> allowedOrigins;

    /**
     * 허용할 HTTP 메서드 목록
     * 예: ["GET", "POST", "PUT", "DELETE", "OPTIONS"]
     */
    private List<String> allowedMethods;

    /**
     * 허용할 헤더 목록
     * 예: ["*"] 또는 ["Content-Type", "Authorization"]
     */
    private List<String> allowedHeaders;

    /**
     * 자격 증명(쿠키, 인증 헤더 등) 포함 허용 여부
     */
    private Boolean allowCredentials = true;

    /**
     * preflight 요청 캐시 시간(초)
     */
    private Long maxAge = 3600L;

    /**
     * 클라이언트에 노출할 헤더 목록
     * 예: ["Set-Cookie", "Authorization"]
     */
    private List<String> exposedHeaders;
}