package com.devon.techblog.presentation;

import com.devon.techblog.service.ChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@WebFluxTest(AiController.class)
class AiControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean(name = "openAiChatService")
    private ChatService chatService;

    @Test
    void chat_withoutStrategy() {
        String prompt = "Hello";
        String expectedResponse = "Hi there!";

        when(chatService.chat(eq(prompt))).thenReturn(Mono.just(expectedResponse));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/ai/chat")
                        .queryParam("prompt", prompt)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo(expectedResponse);
    }

    @Test
    void chat_withStrategy() {
        String prompt = "Hello";
        String strategy = "default";
        String expectedResponse = "Hi there!";

        when(chatService.chat(eq(prompt), eq("defaultPrompt"))).thenReturn(Mono.just(expectedResponse));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/ai/chat")
                        .queryParam("prompt", prompt)
                        .queryParam("strategy", strategy)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo(expectedResponse);
    }

    @Test
    void chatStream_withoutStrategy() {
        String prompt = "Hello";
        Flux<String> responseFlux = Flux.just("Hi", " ", "there", "!");

        when(chatService.chatStream(eq(prompt))).thenReturn(responseFlux);

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/ai/chat/stream")
                        .queryParam("prompt", prompt)
                        .build())
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM)
                .expectBodyList(String.class)
                .hasSize(4);
    }

    @Test
    void chatStream_withStrategy() {
        String prompt = "Hello";
        String strategy = "default";
        Flux<String> responseFlux = Flux.just("Hi", " ", "there", "!");

        when(chatService.chatStream(eq(prompt), eq("defaultPrompt"))).thenReturn(responseFlux);

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/ai/chat/stream")
                        .queryParam("prompt", prompt)
                        .queryParam("strategy", strategy)
                        .build())
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM)
                .expectBodyList(String.class)
                .hasSize(4);
    }
}
