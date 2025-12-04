package com.devon.techblog.service;

import com.devon.techblog.dto.ChatRequest;
import com.devon.techblog.dto.ChatResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiService {

    private final WebClient openaiWebClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

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
                .bodyToFlux(org.springframework.core.io.buffer.DataBuffer.class)
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    return new String(bytes, StandardCharsets.UTF_8);
                })
                .map(chunk -> {
                    StringBuilder result = new StringBuilder();
                    String[] lines = chunk.split("\n");
                    for (String line : lines) {
                        if (line.startsWith("data: ") && !line.contains("[DONE]")) {
                            try {
                                String json = line.substring(6).trim();
                                ChatResponse response = objectMapper.readValue(json, ChatResponse.class);
                                if (response.getChoices() != null && !response.getChoices().isEmpty()) {
                                    ChatResponse.Delta delta = response.getChoices().get(0).getDelta();
                                    if (delta != null && delta.getContent() != null) {
                                        result.append(delta.getContent());
                                    }
                                }
                            } catch (Exception e) {
                            }
                        }
                    }
                    return result.toString();
                })
                .filter(content -> !content.isEmpty());
    }
}
