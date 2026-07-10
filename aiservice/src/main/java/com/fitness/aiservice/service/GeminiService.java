package com.fitness.aiservice.service;

import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class GeminiService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    public GeminiService(WebClient webClient, ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
    }

    public String getAnswer(String question) {
        Map<String, Object> requestBody = Map.of(
                "contents", new Object[] {
                        Map.of("parts", new Object[] {
                                Map.of("text", question)
                        })
                });

        String responseJson = webClient.post()
                .uri(geminiApiUrl)
                .header("Content-Type", "application/json")
                .header("X-goog-api-key", geminiApiKey)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        return extractCleanJson(responseJson);
    }

    private String extractCleanJson(String responseJson) {
        try {
            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && !candidates.isEmpty()) {
                JsonNode firstCandidate = candidates.get(0);
                JsonNode content = firstCandidate.path("content");
                JsonNode parts = content.path("parts");
                if (parts.isArray() && !parts.isEmpty()) {
                    String text = parts.get(0).path("text").asText();
                    
                    // Clean up any markdown code fence blocks if returned by the model
                    if (text.contains("```json")) {
                        text = text.substring(text.indexOf("```json") + 7);
                        if (text.contains("```")) {
                            text = text.substring(0, text.lastIndexOf("```"));
                        }
                    } else if (text.contains("```")) {
                        text = text.substring(text.indexOf("```") + 3);
                        if (text.contains("```")) {
                            text = text.substring(0, text.lastIndexOf("```"));
                        }
                    }
                    return text.trim();
                }
            }
            return responseJson;
        } catch (Exception e) {
            log.error("Failed to parse Gemini response as JSON", e);
            return responseJson;
        }
    }
}
