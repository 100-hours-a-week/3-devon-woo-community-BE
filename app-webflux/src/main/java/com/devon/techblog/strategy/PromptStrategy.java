package com.devon.techblog.strategy;

import com.devon.techblog.dto.ChatRequest;

import java.util.List;

public interface PromptStrategy {
    List<ChatRequest.Message> buildMessages(String userPrompt);
}
