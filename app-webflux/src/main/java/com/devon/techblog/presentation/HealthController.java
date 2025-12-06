package com.devon.techblog.presentation;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class HealthController {

    @GetMapping("/health")
    public Mono<String> health() {
        return Mono.just("WebFlux is running!");
    }
}
