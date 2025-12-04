package com.devon.techblog.strategy;

import com.devon.techblog.dto.ChatRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("defaultPrompt")
public class DefaultPromptStrategy implements PromptStrategy {

    @Override
    public List<ChatRequest.Message> buildMessages(String userPrompt) {
        return List.of(
                ChatRequest.Message.builder()
                        .role("user")
                        .content(userPrompt)
                        .build()
        );
    }
}
