package com.devon.techblog.presentation;

import com.devon.techblog.dto.ReviewRequest;
import com.devon.techblog.service.ChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@WebFluxTest(ReviewController.class)
class ReviewControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ChatService chatService;

    @Test
    void review() {
        ReviewRequest request = new ReviewRequest("This is a text to review");
        String expectedResponse = "Review feedback: Grammar and structure are correct";

        when(chatService.chat(eq(request.text()), eq("reviewPrompt")))
                .thenReturn(Mono.just(expectedResponse));

        webTestClient.post()
                .uri("/ai/review")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo(expectedResponse);
    }

    @Test
    void reviewStream() {
        ReviewRequest request = new ReviewRequest("This is a text to review");
        Flux<String> responseFlux = Flux.just("Review", " ", "feedback", " ", "complete");

        when(chatService.chatStream(eq(request.text()), eq("reviewPrompt")))
                .thenReturn(responseFlux);

        webTestClient.post()
                .uri("/ai/review/stream")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM)
                .expectBodyList(String.class)
                .hasSize(5);
    }
}
