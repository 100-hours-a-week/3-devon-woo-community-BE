package com.devon.techblog.presentation;

import com.devon.techblog.dto.SummarizeRequest;
import com.devon.techblog.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/ai/summarize")
@RequiredArgsConstructor
public class SummarizeController {

    @Qualifier("openAiChatService")
    private final ChatService chatService;

    @PostMapping
    public Mono<String> summarize(@RequestBody SummarizeRequest request) {
        return chatService.chat(request.getText(), "summarizePrompt");
    }
}
