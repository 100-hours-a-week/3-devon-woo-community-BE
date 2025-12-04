package com.devon.techblog.service;

import com.devon.techblog.dto.ChatRequest;
import com.devon.techblog.dto.ChatResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.buffer.DataBuffer;
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

    private static final String GPT_MODEL = "gpt-3.5-turbo";
    private static final String CHAT_COMPLETIONS_URI = "/v1/chat/completions";

    private final WebClient openaiWebClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Mono<String> chatMono(String prompt) {
        ChatRequest request = buildChatRequest(prompt, false);

        return openaiWebClient.post()
                .uri(CHAT_COMPLETIONS_URI)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ChatResponse.class)
                .map(this::extractMessageContent);
    }

    public Flux<String> chatStream(String prompt) {
        ChatRequest request = buildChatRequest(prompt, true);

        return openaiWebClient.post()
                .uri(CHAT_COMPLETIONS_URI)
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(DataBuffer.class)
                .map(this::convertDataBufferToString)
                .map(this::parseStreamChunk)
                .filter(content -> !content.isEmpty());
    }

    private ChatRequest buildChatRequest(String prompt, boolean stream) {
        return ChatRequest.builder()
                .model(GPT_MODEL)
                .messages(List.of(
                        ChatRequest.Message.builder()
                                .role("user")
                                .content(prompt)
                                .build()
                ))
                .stream(stream)
                .build();
    }

    private String extractMessageContent(ChatResponse response) {
        if (response.getChoices() != null && !response.getChoices().isEmpty()) {
            return response.getChoices().get(0).getMessage().getContent();
        }
        return "";
    }

    private String convertDataBufferToString(DataBuffer dataBuffer) {
        byte[] bytes = new byte[dataBuffer.readableByteCount()];
        dataBuffer.read(bytes);
        DataBufferUtils.release(dataBuffer);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private String parseStreamChunk(String chunk) {
        StringBuilder result = new StringBuilder();
        String[] lines = chunk.split("\n");

        for (String line : lines) {
            if (line.startsWith("data: ") && !line.contains("[DONE]")) {
                extractContentFromLine(line).ifPresent(result::append);
            }
        }

        return result.toString();
    }

    private java.util.Optional<String> extractContentFromLine(String line) {
        try {
            String json = line.substring(6).trim();
            ChatResponse response = objectMapper.readValue(json, ChatResponse.class);

            if (response.getChoices() != null && !response.getChoices().isEmpty()) {
                ChatResponse.Delta delta = response.getChoices().get(0).getDelta();
                if (delta != null && delta.getContent() != null) {
                    return java.util.Optional.of(delta.getContent());
                }
            }
        } catch (Exception e) {
        }
        return java.util.Optional.empty();
    }
}
