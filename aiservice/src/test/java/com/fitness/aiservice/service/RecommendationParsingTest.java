package com.fitness.aiservice.service;

import com.fitness.aiservice.model.Activity;
import com.fitness.aiservice.model.Recommendation;
import com.fitness.aiservice.repository.RecommendationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecommendationParsingTest {

    @Mock
    private GeminiService geminiService;

    @Mock
    private RecommendationRepository recommendationRepository;

    @InjectMocks
    private ActivityAiService activityAiService;

    @Test
    void processAiResponse_withStandardJson_parsesCorrectly() {
        // Arrange
        Activity activity = new Activity();
        activity.setId("act-1");
        activity.setUserId("user-1");
        activity.setType("RUNNING");
        activity.setDuration(30);

        String standardJson = "{"
                + "\"analysis\": {"
                + "  \"fitnessScore\": 85,"
                + "  \"intensityLevel\": \"Moderate\","
                + "  \"caloriesBurnt\": 250,"
                + "  \"overallSummary\": \"Excellent run.\""
                + "},"
                + "\"improvements\": ["
                + "  {\"area\": \"Endurance\", \"suggestion\": \"Increase volume\"}"
                + "],"
                + "\"recommendations\": {"
                + "  \"nextWorkout\": \"Active recovery\","
                + "  \"recommendedExercises\": [\"Stretching\"]"
                + "},"
                + "\"warnings\": {"
                + "  \"precautions\": [\"Warm up\"]"
                + "}"
                + "}";

        when(geminiService.getAnswer(anyString())).thenReturn(standardJson);
        when(recommendationRepository.save(any(Recommendation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Recommendation recommendation = activityAiService.generateRecommendation(activity);

        // Assert
        assertNotNull(recommendation);
        assertEquals("act-1", recommendation.getActivityId());
        assertEquals("user-1", recommendation.getUserId());
        assertTrue(recommendation.getRecommendation().contains("Fitness Score: 85"));
        assertTrue(recommendation.getRecommendation().contains("Overall Summary: Excellent run."));
        assertEquals(List.of("Endurance: Increase volume"), recommendation.getImprovements());
        assertEquals(List.of("Next Workout: Active recovery", "Stretching"), recommendation.getSuggestions());
        assertEquals(List.of("Warm up"), recommendation.getSafetyMeasures());
    }

    @Test
    void processAiResponse_withMarkdownFences_parsesCorrectly() {
        // Arrange
        Activity activity = new Activity();
        activity.setId("act-2");
        activity.setUserId("user-1");
        activity.setType("WALKING");

        String fencedJson = "{"
                + "\"candidates\": [{"
                + "  \"content\": {"
                + "    \"parts\": [{"
                + "      \"text\": \"```json\\n{\\n  \\\"analysis\\\": {\\n    \\\"fitnessScore\\\": 60,\\n    \\\"overallSummary\\\": \\\"Good walk.\\\"\\n  }\\n}\\n```\""
                + "    }]"
                + "  }"
                + "}]"
                + "}";

        when(geminiService.getAnswer(anyString())).thenReturn(fencedJson);
        when(recommendationRepository.save(any(Recommendation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Recommendation recommendation = activityAiService.generateRecommendation(activity);

        // Assert
        assertNotNull(recommendation);
        assertTrue(recommendation.getRecommendation().contains("Fitness Score: 60"));
        assertTrue(recommendation.getRecommendation().contains("Overall Summary: Good walk."));
    }
}
