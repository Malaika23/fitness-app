package com.fitness.gateway;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SecurityConfigTest {

    private WebTestClient webTestClient;

    @Autowired
    private ApplicationContext context;

    @BeforeEach
    void setUp() {
        this.webTestClient = WebTestClient.bindToApplicationContext(context)
                .apply(SecurityMockServerConfigurers.springSecurity())
                .build();
    }

    @Test
    void whenRequestMissingJwt_thenUnauthorized() {
        webTestClient.get()
                .uri("/api/activities/user/user-123")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void whenAccessingOwnActivities_thenAuthorized() {
        webTestClient.mutateWith(SecurityMockServerConfigurers.mockJwt()
                        .jwt(jwt -> jwt.subject("user-123")))
                .get()
                .uri("/api/activities/user/user-123")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void whenAccessingOtherUserActivities_thenForbidden() {
        webTestClient.mutateWith(SecurityMockServerConfigurers.mockJwt()
                        .jwt(jwt -> jwt.subject("user-456")))
                .get()
                .uri("/api/activities/user/user-123")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void whenAccessingOwnRecommendation_thenAuthorized() {
        webTestClient.mutateWith(SecurityMockServerConfigurers.mockJwt()
                        .jwt(jwt -> jwt.subject("user-123")))
                .get()
                .uri("/api/recommendation/user/user-123")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void whenAccessingOtherUserRecommendation_thenForbidden() {
        webTestClient.mutateWith(SecurityMockServerConfigurers.mockJwt()
                        .jwt(jwt -> jwt.subject("user-456")))
                .get()
                .uri("/api/recommendation/user/user-123")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void whenPermitAllEndpoint_thenNoAuthRequired() {
        webTestClient.get()
                .uri("/api/user/register")
                .exchange()
                .expectStatus().is5xxServerError(); // Bypasses auth, but fails with 500 because it's a GET without request body
    }
}
