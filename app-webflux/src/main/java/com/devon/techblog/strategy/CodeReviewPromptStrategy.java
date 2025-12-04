package com.devon.techblog.strategy;

import com.devon.techblog.dto.ChatRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("codeReviewPrompt")
public class CodeReviewPromptStrategy implements PromptStrategy {

    private static final String SYSTEM_PROMPT = """
            You are an expert code reviewer.
            Your task is to review code and provide constructive feedback on:
            - Code quality and best practices
            - Potential bugs or issues
            - Performance improvements
            - Security concerns

            Provide clear, actionable suggestions.
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
