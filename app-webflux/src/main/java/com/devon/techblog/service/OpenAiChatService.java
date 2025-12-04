package com.devon.techblog.service;

import com.devon.techblog.config.OpenAiProperties;
import com.devon.techblog.dto.ChatRequest;
import com.devon.techblog.strategy.PromptStrategy;
import com.devon.techblog.util.OpenAiStreamParser;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service("openAiChatService")
@RequiredArgsConstructor
public class OpenAiChatService implements ChatService {

    private final WebClient openaiWebClient;
    private final OpenAiStreamParser streamParser;
    private final OpenAiProperties openAiProperties;
    private final ApplicationContext applicationContext;

    @Override
    public Mono<String> chat(String prompt) {
        return chat(prompt, "");
    }

    @Override
    public Flux<String> chatStream(String prompt) {
        return chatStream(prompt, "");
    }

    @Override
    public Mono<String> chat(String prompt, String strategyName) {
        PromptStrategy strategy = applicationContext.getBean(strategyName, PromptStrategy.class);
        ChatRequest request = buildChatRequest(strategy.buildMessages(prompt), false);

        return openaiWebClient.post()
                .uri(openAiProperties.getApi().getChatCompletionsUri())
                .bodyValue(request)
                .retrieve()
                .bodyToMono(com.devon.techblog.dto.ChatResponse.class)
                .map(streamParser::extractMessageContent);
    }

    @Override
    public Flux<String> chatStream(String prompt, String strategyName) {
        PromptStrategy strategy = applicationContext.getBean(strategyName, PromptStrategy.class);
        ChatRequest request = buildChatRequest(strategy.buildMessages(prompt), true);

        return openaiWebClient.post()
                .uri(openAiProperties.getApi().getChatCompletionsUri())
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(org.springframework.core.io.buffer.DataBuffer.class)
                .map(streamParser::convertDataBufferToString)
                .map(streamParser::parseStreamChunk)
                .filter(content -> !content.isEmpty());
    }

    private ChatRequest buildChatRequest(java.util.List<ChatRequest.Message> messages, boolean stream) {
        return ChatRequest.builder()
                .model(openAiProperties.getModel())
                .messages(messages)
                .stream(stream)
                .build();
    }
}
