package com.fitness.aiservice.service;

import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
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

    @CircuitBreaker(name = "gemini", fallbackMethod = "fallbackGetAnswer")
    @Retry(name = "gemini")
    public String getAnswer(String question) {
        Map<String, Object> requestBody = Map.of(
                "contents", new Object[] {
                        Map.of("parts", new Object[] {
                                Map.of("text", question)
                        })
                },
                "generationConfig", Map.of(
                        "responseMimeType", "application/json",
                        "maxOutputTokens", 1000,
                        "temperature", 0.2
                ));

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

    public String fallbackGetAnswer(String question, Throwable t) {
        log.error("Gemini API call failed (Circuit Breaker/Retry active). Fallback triggered: {}", t.getMessage());
        
        String type = "GENERAL";
        int duration = 30;
        int calories = 200;
        
        try {
            if (question.contains("- Activity Type: ")) {
                int idx = question.indexOf("- Activity Type: ");
                int end = question.indexOf("\n", idx);
                type = question.substring(idx + 17, end).trim();
            }
            if (question.contains("- Duration: ")) {
                int idx = question.indexOf("- Duration: ");
                int end = question.indexOf(" ", idx + 12);
                duration = Integer.parseInt(question.substring(idx + 12, end).trim());
            }
            if (question.contains("- Calories Burned: ")) {
                int idx = question.indexOf("- Calories Burned: ");
                int end = question.indexOf("\n", idx);
                calories = Integer.parseInt(question.substring(idx + 19, end).trim());
            }
        } catch (Exception e) {
            log.warn("Failed to parse activity details from prompt, using general fallbacks", e);
        }

        int score = duration > 45 ? 85 : (duration > 20 ? 70 : 55);
        String intensity = duration > 45 ? "High" : (duration > 20 ? "Moderate" : "Low");
        
        String overallSummary = String.format("A great %d-minute %s session. You maintained consistency and burned %d calories.", 
                duration, type, calories);

        String improvementsJson;
        String suggestionsJson;
        String safetyJson;

        if ("RUNNING".equalsIgnoreCase(type)) {
            improvementsJson = "[{\"area\": \"Endurance\", \"suggestion\": \"Gradually increase weekly volume by 10% to improve performance.\", \"priority\": \"Medium\"}, {\"area\": \"Pacing\", \"suggestion\": \"Practice negative splits during longer runs.\", \"priority\": \"Low\"}]";
            suggestionsJson = "{\"nextWorkout\": \"Active recovery walking or light swimming\", \"recommendedExercises\": [\"Foam rolling\", \"Dynamic leg swings\"]}";
            safetyJson = "{\"precautions\": [\"Avoid running on hard concrete repeatedly\", \"Monitor foot strike and posture\"]}";
        } else if ("WALKING".equalsIgnoreCase(type)) {
            improvementsJson = "[{\"area\": \"Cardio load\", \"suggestion\": \"Increase speed to a brisk pace to raise heart rate.\", \"priority\": \"Medium\"}, {\"area\": \"Posture\", \"suggestion\": \"Keep spine tall and engage core.\", \"priority\": \"Low\"}]";
            suggestionsJson = "{\"nextWorkout\": \"Interval running or bodyweight strength session\", \"recommendedExercises\": [\"Incline treadmill walking\", \"Bodyweight squats\"]}";
            safetyJson = "{\"precautions\": [\"Wear supportive athletic shoes\", \"Stay alert to your surroundings\"]}";
        } else if ("CYCLING".equalsIgnoreCase(type)) {
            improvementsJson = "[{\"area\": \"Cadence\", \"suggestion\": \"Aim for 80-90 RPM to protect knees.\", \"priority\": \"High\"}, {\"area\": \"Posture\", \"suggestion\": \"Ensure optimal seat height to maximize power.\", \"priority\": \"Medium\"}]";
            suggestionsJson = "{\"nextWorkout\": \"Core stability and upper body strength\", \"recommendedExercises\": [\"Planks and side planks\", \"Hip flexor stretches\"]}";
            safetyJson = "{\"precautions\": [\"Always wear a helmet\", \"Check tire pressure before riding\"]}";
        } else {
            improvementsJson = "[{\"area\": \"Consistency\", \"suggestion\": \"Schedule 3 structured sessions per week.\", \"priority\": \"Medium\"}, {\"area\": \"Intensity\", \"suggestion\": \"Focus on maintaining target heart rate zones.\", \"priority\": \"Low\"}]";
            suggestionsJson = "{\"nextWorkout\": \"Moderate-intensity cardio or mobility flow\", \"recommendedExercises\": [\"Light stretching\", \"Yoga session\"]}";
            safetyJson = "{\"precautions\": [\"Always warm up and cool down\", \"Listen to your body and rest when needed\"]}";
        }

        return String.format("""
            {
              "analysis": {
                "fitnessScore": %d,
                "intensityLevel": "%s",
                "caloriesBurnt": %d,
                "overallSummary": "%s"
              },
              "improvements": %s,
              "recommendations": %s,
              "warnings": %s
            }
            """, score, intensity, calories, overallSummary, improvementsJson, suggestionsJson, safetyJson);
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
