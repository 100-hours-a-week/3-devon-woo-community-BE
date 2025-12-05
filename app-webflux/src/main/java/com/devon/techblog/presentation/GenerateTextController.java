package com.devon.techblog.presentation;

import com.devon.techblog.dto.GenerateTextRequest;
import com.devon.techblog.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/ai/generate")
@RequiredArgsConstructor
public class GenerateTextController {

    private final ChatService chatService;

    /**
     * {텍스트 작성} 플레이스홀더를 문맥과 명령에 맞춰 생성
     */
    @PostMapping
    public Mono<String> generateText(@RequestBody GenerateTextRequest request) {
        String userPrompt = String.format("""
                Content: %s

                Instruction: %s
                """, request.content(), request.instruction());

        return chatService.chat(userPrompt, "generateTextPrompt");
    }

    /**
     * {텍스트 작성} 플레이스홀더를 문맥과 명령에 맞춰 생성 (스트리밍)
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> generateTextStream(@RequestBody GenerateTextRequest request) {
        String userPrompt = String.format("""
                Content: %s

                Instruction: %s
                """, request.content(), request.instruction());

        return chatService.chatStream(userPrompt, "generateTextPrompt")
                .map(content -> ServerSentEvent.<String>builder().data(content).build());
    }
}
