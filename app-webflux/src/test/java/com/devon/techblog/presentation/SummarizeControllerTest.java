package com.devon.techblog.presentation;

import com.devon.techblog.config.TestSecurityConfig;
import com.devon.techblog.dto.SummarizeRequest;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@WebFluxTest(SummarizeController.class)
@Import(TestSecurityConfig.class)
class SummarizeControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private ChatService chatService;

    @Test
    void summarize() {
        SummarizeRequest request = new SummarizeRequest("This is a long text that needs to be summarized");
        String expectedResponse = "Summarized text in 50-100 characters";

        when(chatService.chat(eq(request.text()), eq("summarizePrompt")))
                .thenReturn(Mono.just(expectedResponse));

        webTestClient.post()
                .uri("/ai/summarize")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo(expectedResponse);
    }

    @Test
    void summarizeStream() {
        SummarizeRequest request = new SummarizeRequest("This is a long text that needs to be summarized");
        Flux<String> responseFlux = Flux.just("Summarized", "text", "response");

        when(chatService.chatStream(eq(request.text()), eq("summarizePrompt")))
                .thenReturn(responseFlux);

        webTestClient.post()
                .uri("/ai/summarize/stream")
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
