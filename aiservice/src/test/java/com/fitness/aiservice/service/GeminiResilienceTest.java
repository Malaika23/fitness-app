package com.fitness.aiservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
    "GEMINI_API_URL=http://mock-gemini-url",
    "GEMINI_API_KEY=mock-api-key",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration,org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration,org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration",
    "resilience4j.retry.instances.gemini.maxAttempts=3",
    "resilience4j.retry.instances.gemini.waitDuration=10ms",
    "resilience4j.circuitbreaker.instances.gemini.slidingWindowSize=5",
    "resilience4j.circuitbreaker.instances.gemini.failureRateThreshold=50",
    "resilience4j.circuitbreaker.circuitBreakerAspectOrder=1",
    "resilience4j.retry.retryAspectOrder=2"
})
class GeminiResilienceTest {

    @Autowired
    private GeminiService geminiService;

    @MockitoBean
    private WebClient webClient;

    private WebClient.RequestBodyUriSpec requestBodyUriSpec;
    private WebClient.RequestBodySpec requestBodySpec;
    @SuppressWarnings("rawtypes")
    private WebClient.RequestHeadersSpec requestHeadersSpec;
    private WebClient.ResponseSpec responseSpec;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        requestBodySpec = mock(WebClient.RequestBodySpec.class);
        requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    void whenGeminiThrowsError_fallbackIsTriggered() {
        // Arrange
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.error(new RuntimeException("API Connection Timeout")));

        // Act
        String result = geminiService.getAnswer("Test question");

        // Assert
        assertTrue(result.contains("overallSummary"));
        assertTrue(result.contains("A great 30-minute GENERAL session"));
        
        // Verify retry attempts occurred (since retry max is 3, total attempts should be 3)
        verify(requestHeadersSpec, atLeast(2)).retrieve();
    }
}
