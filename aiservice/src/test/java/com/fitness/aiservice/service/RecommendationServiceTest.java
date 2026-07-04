package com.fitness.aiservice.service;

import com.fitness.aiservice.model.Recommendation;
import com.fitness.aiservice.repository.RecommendationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private RecommendationRepository recommendationRepository;

    @InjectMocks
    private RecommendationService recommendationService;

    @Test
    void saveRecommendation_savesSuccessfully() {
        // Arrange
        Recommendation recommendation = new Recommendation();
        recommendation.setId("rec1");
        recommendation.setUserId("user1");
        recommendation.setRecommendation("Do active stretching.");

        when(recommendationRepository.save(any(Recommendation.class))).thenReturn(recommendation);

        // Act
        recommendationService.saveRecommendation(recommendation);

        // Assert
        verify(recommendationRepository, times(1)).save(recommendation);
    }

    @Test
    void getUserRecommendations_whenExist_returnsList() {
        // Arrange
        String userId = "user1";
        Recommendation rec = new Recommendation();
        rec.setId("rec1");
        rec.setUserId(userId);

        when(recommendationRepository.findByUserId(userId)).thenReturn(List.of(rec));

        // Act
        List<Recommendation> result = recommendationService.getUserRecommendations(userId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("rec1", result.get(0).getId());
        verify(recommendationRepository, times(1)).findByUserId(userId);
    }

    @Test
    void getUserRecommendations_whenNone_returnsEmptyList() {
        // Arrange
        String userId = "user-empty";
        when(recommendationRepository.findByUserId(userId)).thenReturn(Collections.emptyList());

        // Act
        List<Recommendation> result = recommendationService.getUserRecommendations(userId);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(recommendationRepository, times(1)).findByUserId(userId);
    }

    @Test
    void getActivityRecommendations_whenExist_returnsRecommendation() {
        // Arrange
        String activityId = "act1";
        Recommendation rec = new Recommendation();
        rec.setId("rec1");
        rec.setActivityId(activityId);

        when(recommendationRepository.findByActivityId(activityId)).thenReturn(Optional.of(rec));

        // Act
        Recommendation result = recommendationService.getActivityRecommendations(activityId);

        // Assert
        assertNotNull(result);
        assertEquals("rec1", result.getId());
        verify(recommendationRepository, times(1)).findByActivityId(activityId);
    }

    @Test
    void getActivityRecommendations_whenNotFound_throwsRuntimeException() {
        // Arrange
        String activityId = "missing-act";
        when(recommendationRepository.findByActivityId(activityId)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            recommendationService.getActivityRecommendations(activityId);
        });

        assertEquals("Recommendation Not Found for activity Id missing-act", exception.getMessage());
        verify(recommendationRepository, times(1)).findByActivityId(activityId);
    }
}
