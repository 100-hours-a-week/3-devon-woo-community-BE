package com.devon.techblog.presentation;

import com.devon.techblog.dto.GenerateTextRequest;
import com.devon.techblog.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/ai/generate")
@RequiredArgsConstructor
public class GenerateTextController {

    @Qualifier("openAiChatService")
    private final ChatService chatService;

    @PostMapping
    public Mono<String> generateText(@RequestBody GenerateTextRequest request) {
        String userPrompt = String.format("""
                Content: %s

                Instruction: %s
                """, request.getContent(), request.getInstruction());

        return chatService.chat(userPrompt, "generateTextPrompt");
    }
}
