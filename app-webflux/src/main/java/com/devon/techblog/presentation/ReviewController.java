package com.devon.techblog.presentation;

import com.devon.techblog.dto.ReviewRequest;
import com.devon.techblog.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/ai/review")
@RequiredArgsConstructor
public class ReviewController {

    private final ChatService chatService;

    /**
     * 문법, 구조를 검토하고 피드백 제공
     */
    @PostMapping
    public Mono<String> review(@RequestBody ReviewRequest request) {
        return chatService.chat(request.getText(), "reviewPrompt");
    }
}
