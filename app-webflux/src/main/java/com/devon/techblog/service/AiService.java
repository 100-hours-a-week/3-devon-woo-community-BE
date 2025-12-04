package com.devon.techblog.service;

import com.devon.techblog.dto.ChatRequest;
import com.devon.techblog.dto.ChatResponse;
import com.devon.techblog.util.OpenAiStreamParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AiService {

    private static final String GPT_MODEL = "gpt-3.5-turbo";
    private static final String CHAT_COMPLETIONS_URI = "/v1/chat/completions";

    private final WebClient openaiWebClient;
    private final OpenAiStreamParser streamParser;

    public Mono<String> chatMono(String prompt) {
        ChatRequest request = buildChatRequest(prompt, false);

        return openaiWebClient.post()
                .uri(CHAT_COMPLETIONS_URI)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ChatResponse.class)
                .map(streamParser::extractMessageContent);
    }

    public Flux<String> chatStream(String prompt) {
        ChatRequest request = buildChatRequest(prompt, true);

        return openaiWebClient.post()
                .uri(CHAT_COMPLETIONS_URI)
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(org.springframework.core.io.buffer.DataBuffer.class)
                .map(streamParser::convertDataBufferToString)
                .map(streamParser::parseStreamChunk)
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
}
