package com.devon.techblog.strategy;

import com.devon.techblog.dto.ChatRequest;
import com.devon.techblog.strategy.prompt.GenerateTextPrompt;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("generateTextPrompt")
public class GenerateTextPromptStrategy implements PromptStrategy {

    @Override
    public List<ChatRequest.Message> buildMessages(String userPrompt) {
        return List.of(
                ChatRequest.Message.builder()
                        .role("system")
                        .content(GenerateTextPrompt.SYSTEM_PROMPT)
                        .build(),
                ChatRequest.Message.builder()
                        .role("user")
                        .content(userPrompt)
                        .build()
        );
    }
}
