package com.devon.techblog.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${openai.api.url:https://api.openai.com}")
    private String openaiApiUrl;

    @Value("${openai.api.key}")
    private String openaiApiKey;

    @Bean
    public WebClient openaiWebClient() {
        return WebClient.builder()
                .baseUrl(openaiApiUrl)
                .defaultHeader("Authorization", "Bearer " + openaiApiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
