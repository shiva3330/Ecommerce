package com.marketplace.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "CORS_ALLOWED_ORIGINS=http://localhost:4200")
class ApiGatewayCorsTest {

    @LocalServerPort
    private int port;

    @Test
    void allowsTheStorefrontToPreflightCheckoutWithAnIdempotencyKey() {
        WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build()
                .options()
                .uri("/api/v1/orders")
                .header("Origin", "http://localhost:4200")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "content-type,idempotency-key")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("Access-Control-Allow-Origin", "http://localhost:4200")
                .expectHeader().valueMatches("Access-Control-Allow-Headers", "(?i).*Idempotency-Key.*");
    }

    @Test
    void rejectsUntrustedOrigins() {
        WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build()
                .options().uri("/api/v1/orders")
                .header("Origin", "https://untrusted.example")
                .header("Access-Control-Request-Method", "POST")
                .exchange()
                .expectStatus().isForbidden()
                .expectHeader().doesNotExist("Access-Control-Allow-Origin");
    }
}
