package com.fitness.aiservice.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Arrays;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitness.aiservice.model.Activity;
import com.fitness.aiservice.model.Recommendation;
import com.fitness.aiservice.repository.RecommendationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ActivityAiService {

    private final GeminiService geminiService;
    private final RecommendationRepository recommendationRepository;

    public Recommendation generateRecommendation(Activity activity) {
        try {
            String prompt = createPromptForActivity(activity);
            String aiResponse = geminiService.getAnswer(prompt);
            log.info("RESPONSE FROM AI: {} ", aiResponse);
            return processAiResponse(activity, aiResponse);
        } catch (Exception e) {
            log.error("Failed to generate recommendation, using default", e);
            return createDefaultRecommendation(activity);
        }
    }

    private Recommendation processAiResponse(Activity activity, String aiResponse) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(aiResponse);

            JsonNode targetNode = rootNode;
            // Check if this is the wrapped Gemini API response
            if (rootNode.has("candidates")) {
                JsonNode textNode = rootNode.path("candidates")
                                    .path(0)
                                    .path("content")
                                    .path("parts")
                                    .path(0)
                                    .path("text");
                String jsonContent = textNode.asText()
                                    .replaceAll("```json\\n","")
                                    .replaceAll("```\\n","")
                                    .replaceAll("\\n```","")
                                    .trim();
                targetNode = mapper.readTree(jsonContent);
            }

            JsonNode analysisJson = targetNode;
            JsonNode analysisNode = analysisJson.path("analysis");

            StringBuilder fullAnalysis = new StringBuilder();
            addAnalysisSection(fullAnalysis, analysisNode, "overallSummary", "Overall Summary:");
            addAnalysisSection(fullAnalysis, analysisNode, "fitnessScore", "Fitness Score:");
            addAnalysisSection(fullAnalysis, analysisNode, "intensityLevel", "Intensity Level:");
            addAnalysisSection(fullAnalysis, analysisNode, "caloriesBurnt", "Calories Burnt:");
            addAnalysisSection(fullAnalysis, analysisNode, "overall", "Overall:");
            addAnalysisSection(fullAnalysis, analysisNode, "pace", "Pace:");
            addAnalysisSection(fullAnalysis, analysisNode, "heartRate", "Heart Rate:");
            addAnalysisSection(fullAnalysis, analysisNode, "caloriesBurned", "Calories:");

            List<String> improvements = extractImprovements(analysisJson.path("improvements"));
            List<String> suggestions = extractSuggestions(analysisJson.path("recommendations"));
            List<String> safety = extractSafetyGuidelines(analysisJson.path("warnings"));

            Recommendation recommendation = Recommendation.builder()
                    .activityId(activity.getId())
                    .userId(activity.getUserId())
                    .activityType(activity.getType())
                    .recommendation(fullAnalysis.toString().trim())
                    .improvements(improvements)
                    .suggestions(suggestions)
                    .safetyMeasures(safety)
                    .createdAt(LocalDateTime.now())
                    .build();

            return recommendationRepository.save(recommendation);

        } catch (Exception e) {
            log.error("Error processing AI response, falling back to default", e);
            return createDefaultRecommendation(activity);
        }
    }

    private Recommendation createDefaultRecommendation(Activity activity) {
        String type = activity.getType() != null ? activity.getType() : "GENERAL";
        int duration = activity.getDuration() != null ? activity.getDuration() : 30;
        int calories = activity.getCaloriesBurned() != null ? activity.getCaloriesBurned() : 200;
        
        int score = duration > 45 ? 85 : (duration > 20 ? 70 : 55);
        String intensity = duration > 45 ? "High" : (duration > 20 ? "Moderate" : "Low");
        
        String analysis = String.format("Overall Summary: A great %d-minute %s session. You maintained consistency and burned %d calories. Fitness Score: %d. Intensity Level: %s.", 
                duration, type, calories, score, intensity);

        List<String> improvements = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();
        List<String> safety = new ArrayList<>();

        if ("RUNNING".equalsIgnoreCase(type)) {
            improvements.add("Endurance: Gradually increase weekly volume by 10% to improve performance.");
            improvements.add("Pacing: Practice negative splits during longer runs.");
            suggestions.add("Next Workout: Active recovery walking or light swimming");
            suggestions.add("Foam rolling");
            suggestions.add("Dynamic leg swings");
            safety.add("Avoid running on hard concrete repeatedly");
            safety.add("Monitor foot strike and posture");
        } else if ("WALKING".equalsIgnoreCase(type)) {
            improvements.add("Cardio load: Increase speed to a brisk pace to raise heart rate.");
            improvements.add("Postures: Keep spine tall and engage core.");
            suggestions.add("Next Workout: Interval running or bodyweight strength session");
            suggestions.add("Incline treadmill walking");
            suggestions.add("Bodyweight squats");
            safety.add("Wear supportive athletic shoes");
            safety.add("Stay alert to your surroundings");
        } else if ("CYCLING".equalsIgnoreCase(type)) {
            improvements.add("Cadence: Aim for 80-90 RPM to protect knees.");
            improvements.add("Postures: Ensure optimal seat height to maximize power.");
            suggestions.add("Next Workout: Core stability and upper body strength");
            suggestions.add("Planks and side planks");
            suggestions.add("Hip flexor stretches");
            safety.add("Always wear a helmet");
            safety.add("Check tire pressure before riding");
        } else {
            improvements.add("Consistency: Schedule 3 structured sessions per week.");
            improvements.add("Intensity: Focus on maintaining target heart rate zones.");
            suggestions.add("Next Workout: Moderate-intensity cardio or mobility flow");
            suggestions.add("Light stretching");
            suggestions.add("Yoga session");
            safety.add("Always warm up and cool down");
            safety.add("Listen to your body and rest when needed");
        }

        safety.add("Stay hydrated");

        Recommendation recommendation = Recommendation.builder()
                .activityId(activity.getId())
                .userId(activity.getUserId())
                .activityType(activity.getType())
                .recommendation(analysis)
                .improvements(improvements)
                .suggestions(suggestions)
                .safetyMeasures(safety)
                .createdAt(LocalDateTime.now())
                .build();

        return recommendationRepository.save(recommendation);
    }

    private List<String> extractSafetyGuidelines(JsonNode warningsNode) {
        List<String> safety = new ArrayList<>();
        JsonNode precautions = warningsNode.path("precautions");
        if (precautions.isArray()) {
            precautions.forEach(item -> safety.add(item.asText()));
        } else if (warningsNode.isArray()) { // fallback if warningsNode itself is an array
            warningsNode.forEach(item -> safety.add(item.asText()));
        }
        return safety.isEmpty() ?
                Collections.singletonList("Follow general safety guidelines") :
                safety;
    }

    private List<String> extractSuggestions(JsonNode recommendationsNode) {
        List<String> suggestions = new ArrayList<>();
        JsonNode exercises = recommendationsNode.path("recommendedExercises");
        if (exercises.isArray()) {
            exercises.forEach(ex -> suggestions.add(ex.asText()));
        } else if (recommendationsNode.isArray()) { // fallback if structure is different
            recommendationsNode.forEach(suggestion -> {
                String workout = suggestion.path("workout").asText();
                String description = suggestion.path("description").asText();
                suggestions.add(String.format("%s: %s", workout, description));
            });
        }
        
        String nextWorkout = recommendationsNode.path("nextWorkout").asText();
        if (!nextWorkout.isEmpty()) {
            suggestions.add(0, "Next Workout: " + nextWorkout);
        }
        
        return suggestions.isEmpty() ?
                Collections.singletonList("No specific suggestions provided") :
                suggestions;
    }

    private List<String> extractImprovements(JsonNode improvementsNode) {
        List<String> improvements = new ArrayList<>();
        if (improvementsNode.isArray()) {
            improvementsNode.forEach(improvement -> {
                String area = improvement.path("area").asText();
                String detail = improvement.path("suggestion").asText();
                if (detail.isEmpty()) {
                    detail = improvement.path("recommendation").asText();
                }
                if (!area.isEmpty() || !detail.isEmpty()) {
                    improvements.add(String.format("%s: %s", area, detail));
                }
            });
        }
        return improvements.isEmpty() ?
                Collections.singletonList("No specific improvements provided") :
                improvements;
    }

    private void addAnalysisSection(StringBuilder fullAnalysis, JsonNode analysisNode, String key, String prefix) {
        if (!analysisNode.path(key).isMissingNode() && !analysisNode.path(key).asText().isEmpty()) {
            fullAnalysis.append(prefix)
                    .append(" ")
                    .append(analysisNode.path(key).asText())
                    .append(" ");
        }
    }

    @SuppressWarnings("unused")
    private String generateFallbackRecommendation(Activity activity) {
        int duration = activity.getDuration() != null ? activity.getDuration() : 30;
        int calories = activity.getCaloriesBurned() != null ? activity.getCaloriesBurned() : 250;
        String intensity = duration > 45 ? "High" : (duration > 20 ? "Moderate" : "Low");
        int score = duration > 30 ? 85 : 70;

        return String.format("""
            {
              "analysis": {
                "fitnessScore": %d,
                "intensityLevel": "%s",
                "caloriesBurnt": %d,
                "areasWorked": ["Cardiovascular System", "Lower Body", "Core"],
                "formAnalysis": "Form looks solid. Focus on steady rhythmic breathing and midfoot strike.",
                "consistency": "Good",
                "overallSummary": "Great %d-minute session. You maintained a steady energy output and burned %d calories."
              },
              "improvements": [
                {
                  "area": "Endurance",
                  "suggestion": "Gradually increase weekly volume by 10%%.",
                  "priority": "Medium"
                }
              ],
              "recommendations": {
                "nextWorkout": "Active Recovery / Mobility Session",
                "recommendedExercises": ["Foam rolling", "Dynamic stretching"],
                "recommendedDuration": "20 minutes",
                "recommendedIntensity": "Low",
                "recoveryAdvice": "Hydrate well and stretch post-session.",
                "hydrationAdvice": "Aim to drink 500-700ml of water with electrolytes.",
                "nutritionAdvice": "Refuel with a mix of carbohydrates and lean proteins within 45 minutes.",
                "stretchingExercises": ["Hamstring stretch", "Calf stretch", "Quad stretch"],
                "weeklyGoal": "Target 3 consistent sessions of similar intensity."
              },
              "warnings": {
                "injuryRisk": "Low",
                "overtrainingRisk": "Low",
                "precautions": ["Always warm up and cool down properly."]
              }
            }
            """, score, intensity, calories, duration, calories);
    }

    private String createPromptForActivity(Activity activity) {
        String activityTypeStr = activity.getType();
        if ("OTHER".equalsIgnoreCase(activityTypeStr) && activity.getAdditionalMetrics() != null && activity.getAdditionalMetrics().containsKey("customType")) {
            activityTypeStr = "OTHER (" + activity.getAdditionalMetrics().get("customType") + ")";
        }
        return String.format("""
            You are an expert fitness coach, sports scientist, and physiotherapist.

            Analyze the following fitness activity and provide personalized recommendations.

            Activity Details:
            - Activity Type: %s
            - Duration: %d minutes
            - Calories Burned: %d
            - Start Time: %s
            - Additional Metrics: %s

            Your response MUST be valid JSON only.

            Use the following schema:

            {
            "analysis": {
                "fitnessScore": 0,
                "intensityLevel": "Low | Moderate | High",
                "caloriesBurnt": 0,
                "areasWorked": [],
                "formAnalysis": "",
                "consistency": "",
                "overallSummary": ""
            },
            "improvements": [
                {
                "area": "",
                "suggestion": "",
                "priority": "High | Medium | Low"
                }
            ],
            "recommendations": {
                "nextWorkout": "",
                "recommendedExercises": [],
                "recommendedDuration": "",
                "recommendedIntensity": "",
                "recoveryAdvice": "",
                "hydrationAdvice": "",
                "nutritionAdvice": "",
                "stretchingExercises": [],
                "weeklyGoal": ""
            },
            "warnings": {
                "injuryRisk": "",
                "overtrainingRisk": "",
                "precautions": []
            }
            }

            Rules:
            - Give a fitnessScore between 0 and 100.
            - Estimate caloriesBurnt using the activity duration, intensity, and type.
            - Identify the major muscle groups worked.
            - Suggest practical improvements.
            - Recommend the next workout based on recovery principles.
            - If the workout intensity is very high, recommend recovery instead of another intense session.
            - If the activity is too short or inconsistent, explain why.
            - Keep recommendations realistic and evidence-based.
            - Do not include markdown, explanations, or code fences.
            - Each field should be concise.
            - Maximum 2 sentences for any text field.
            - Maximum 40 words for recommendations.
            - Keep the response suitable for a mobile fitness application.
            - Avoid repeating information already present in other fields.
            - Return clean JSON only.

            """,
            activityTypeStr,
            activity.getDuration() != null ? activity.getDuration() : 30,
            activity.getCaloriesBurned() != null ? activity.getCaloriesBurned() : 200,
            activity.getStartTime() != null ? activity.getStartTime().toString() : "N/A",
            activity.getAdditionalMetrics() != null ? activity.getAdditionalMetrics().toString() : "{}"
        );
    }
}
