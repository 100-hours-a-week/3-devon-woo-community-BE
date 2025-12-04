package com.devon.techblog.presentation;

import com.devon.techblog.dto.SummarizeRequest;
import com.devon.techblog.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/ai/summarize")
@RequiredArgsConstructor
public class SummarizeController {

    private final ChatService chatService;

    /**
     * 텍스트를 50-100자로 요약
     */
    @PostMapping
    public Mono<String> summarize(@RequestBody SummarizeRequest request) {
        return chatService.chat(request.text(), "summarizePrompt");
    }

    /**
     * 텍스트를 50-100자로 요약 (스트리밍)
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> summarizeStream(@RequestBody SummarizeRequest request) {
        return chatService.chatStream(request.text(), "summarizePrompt")
                .map(content -> ServerSentEvent.<String>builder().data(content).build());
    }
}
