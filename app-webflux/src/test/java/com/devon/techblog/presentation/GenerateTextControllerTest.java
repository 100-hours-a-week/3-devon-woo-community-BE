package com.devon.techblog.presentation;

import com.devon.techblog.config.TestSecurityConfig;
import com.devon.techblog.dto.GenerateTextRequest;
import com.devon.techblog.service.ChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@WebFluxTest(GenerateTextController.class)
@Import(TestSecurityConfig.class)
class GenerateTextControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private ChatService chatService;

    @Test
    void generateText() {
        GenerateTextRequest request = new GenerateTextRequest("Sample content", "Write a summary");
        String expectedResponse = "Generated text based on content and instruction";

        when(chatService.chat(anyString(), eq("generateTextPrompt")))
                .thenReturn(Mono.just(expectedResponse));

        webTestClient.post()
                .uri("/ai/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo(expectedResponse);
    }

    @Test
    void generateTextStream() {
        GenerateTextRequest request = new GenerateTextRequest("Sample content", "Write a summary");
        Flux<String> responseFlux = Flux.just("Generated", "text", "response");

        when(chatService.chatStream(anyString(), eq("generateTextPrompt")))
                .thenReturn(responseFlux);

        webTestClient.post()
                .uri("/ai/generate/stream")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM)
                .expectBodyList(String.class)
                .hasSize(3);
    }
}
