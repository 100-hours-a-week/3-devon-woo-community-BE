package com.devon.techblog.strategy;

import com.devon.techblog.dto.ChatRequest;
import com.devon.techblog.strategy.prompt.SummarizePrompt;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("summarizePrompt")
public class SummarizePromptStrategy implements PromptStrategy {

    @Override
    public List<ChatRequest.Message> buildMessages(String userPrompt) {
        return List.of(
                ChatRequest.Message.builder()
                        .role("system")
                        .content(SummarizePrompt.SYSTEM_PROMPT)
                        .build(),
                ChatRequest.Message.builder()
                        .role("user")
                        .content(userPrompt)
                        .build()
        );
    }
}
