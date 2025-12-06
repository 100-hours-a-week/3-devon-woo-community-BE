package com.devon.techblog.config.properties;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "spring.security.cors")
public class CorsProperties {

    private List<String> allowedOrigins;
    private List<String> allowedMethods;
    private List<String> allowedHeaders;
    private Boolean allowCredentials = true;
    private Long maxAge = 3600L;
    private List<String> exposedHeaders;
}
