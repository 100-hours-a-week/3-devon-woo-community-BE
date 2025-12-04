package com.devon.techblog.service;

import com.devon.techblog.dto.ChatRequest;
import com.devon.techblog.dto.ChatResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AiService {

    private final WebClient openaiWebClient;

    public Mono<String> chatMono(String prompt) {
        ChatRequest request = ChatRequest.builder()
                .model("gpt-3.5-turbo")
                .messages(List.of(
                        ChatRequest.Message.builder()
                                .role("user")
                                .content(prompt)
                                .build()
                ))
                .stream(false)
                .build();

        return openaiWebClient.post()
                .uri("/v1/chat/completions")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ChatResponse.class)
                .map(response -> {
                    if (response.getChoices() != null && !response.getChoices().isEmpty()) {
                        return response.getChoices().get(0).getMessage().getContent();
                    }
                    return "";
                });
    }

    public Flux<String> chatStream(String prompt) {
        ChatRequest request = ChatRequest.builder()
                .model("gpt-3.5-turbo")
                .messages(List.of(
                        ChatRequest.Message.builder()
                                .role("user")
                                .content(prompt)
                                .build()
                ))
                .stream(true)
                .build();

        return openaiWebClient.post()
                .uri("/v1/chat/completions")
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(String.class)
                .filter(line -> line.startsWith("data: ") && !line.contains("[DONE]"))
                .map(line -> line.substring(6))
                .flatMap(json -> {
                    try {
                        return Mono.just(json);
                    } catch (Exception e) {
                        return Mono.empty();
                    }
                });
    }
}
