package com.devon.techblog.presentation;

import com.devon.techblog.config.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;

@WebFluxTest(HealthController.class)
@Import(TestSecurityConfig.class)
class HealthControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void health() {
        webTestClient.get()
                .uri("/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo("WebFlux is running!");
    }
}
