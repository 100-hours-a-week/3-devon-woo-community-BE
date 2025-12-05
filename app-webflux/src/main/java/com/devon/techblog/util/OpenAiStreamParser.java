package com.devon.techblog.util;

import com.devon.techblog.dto.ChatResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OpenAiStreamParser {

    private final ObjectMapper objectMapper;

    public String extractMessageContent(ChatResponse response) {
        if (response.getChoices() != null && !response.getChoices().isEmpty()) {
            return response.getChoices().getFirst().getMessage().getContent();
        }
        return "";
    }

    public String convertDataBufferToString(DataBuffer dataBuffer) {
        byte[] bytes = new byte[dataBuffer.readableByteCount()];
        dataBuffer.read(bytes);
        DataBufferUtils.release(dataBuffer);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public String parseStreamChunk(String chunk) {
        StringBuilder result = new StringBuilder();
        String[] lines = chunk.split("\n");

        for (String line : lines) {
            if (line.startsWith("data: ") && !line.contains("[DONE]")) {
                extractContentFromLine(line).ifPresent(result::append);
            }
        }

        return result.toString();
    }

    private Optional<String> extractContentFromLine(String line) {
        try {
            String json = line.substring(6);
            ChatResponse response = objectMapper.readValue(json, ChatResponse.class);

            if (response.getChoices() != null && !response.getChoices().isEmpty()) {
                ChatResponse.Delta delta = response.getChoices().getFirst().getDelta();
                if (delta != null && delta.getContent() != null) {
                    return Optional.of(delta.getContent());
                }
            }
        } catch (Exception ignored) {
        }
        return Optional.empty();
    }
}
