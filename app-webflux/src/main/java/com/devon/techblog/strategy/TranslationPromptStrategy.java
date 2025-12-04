package com.devon.techblog.strategy;

import com.devon.techblog.dto.ChatRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("translationPrompt")
public class TranslationPromptStrategy implements PromptStrategy {

    private static final String SYSTEM_PROMPT = """
            You are a professional translator.
            Translate the given text accurately while maintaining:
            - Original meaning and context
            - Natural language flow
            - Cultural nuances

            Only provide the translation without explanations.
            """;

    @Override
    public List<ChatRequest.Message> buildMessages(String userPrompt) {
        return List.of(
                ChatRequest.Message.builder()
                        .role("system")
                        .content(SYSTEM_PROMPT)
                        .build(),
                ChatRequest.Message.builder()
                        .role("user")
                        .content(userPrompt)
                        .build()
        );
    }
}
