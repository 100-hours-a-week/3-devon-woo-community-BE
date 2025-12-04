package com.devon.techblog.service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ChatService {
    Mono<String> chat(String prompt);
    Flux<String> chatStream(String prompt);
}
