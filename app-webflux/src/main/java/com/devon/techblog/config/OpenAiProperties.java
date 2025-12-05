package com.devon.techblog.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "openai")
public class OpenAiProperties {
    private String model = "gpt-3.5-turbo";
    private Api api = new Api();

    @Getter
    @Setter
    public static class Api {
        private String url;
        private String key;
        private String chatCompletionsUri = "/v1/chat/completions";
    }
}
