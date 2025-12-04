package com.devon.techblog.service;

import com.devon.techblog.config.OpenAiProperties;
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

    private final WebClient openaiWebClient;
    private final OpenAiStreamParser streamParser;
    private final OpenAiProperties openAiProperties;

    public Mono<String> chatMono(String prompt) {
        ChatRequest request = buildChatRequest(prompt, false);

        return openaiWebClient.post()
                .uri(openAiProperties.getApi().getChatCompletionsUri())
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ChatResponse.class)
                .map(streamParser::extractMessageContent);
    }

    public Flux<String> chatStream(String prompt) {
        ChatRequest request = buildChatRequest(prompt, true);

        return openaiWebClient.post()
                .uri(openAiProperties.getApi().getChatCompletionsUri())
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(org.springframework.core.io.buffer.DataBuffer.class)
                .map(streamParser::convertDataBufferToString)
                .map(streamParser::parseStreamChunk)
                .filter(content -> !content.isEmpty());
    }

    private ChatRequest buildChatRequest(String prompt, boolean stream) {
        return ChatRequest.builder()
                .model(openAiProperties.getModel())
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
