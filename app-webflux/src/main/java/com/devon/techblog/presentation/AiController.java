package com.devon.techblog.presentation;

import com.devon.techblog.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiController {

    @Qualifier("openAiChatService")
    private final ChatService chatService;

    @GetMapping("/chat")
    public Mono<String> chat(@RequestParam String prompt) {
        return chatService.chat(prompt);
    }

    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatStream(@RequestParam String prompt) {
        return chatService.chatStream(prompt)
                .map(content -> ServerSentEvent.<String>builder().data(content).build());
    }
}
