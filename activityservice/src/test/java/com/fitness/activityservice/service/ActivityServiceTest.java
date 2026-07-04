package com.fitness.activityservice.service;

import com.fitness.activityservice.controller.ActivityRequest;
import com.fitness.activityservice.controller.ActivityResponse;
import com.fitness.activityservice.exception.ActivityNotFoundException;
import com.fitness.activityservice.model.Activity;
import com.fitness.activityservice.model.ActivityType;
import com.fitness.activityservice.repository.ActivityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivityServiceTest {

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private UserValidationService userValidationService;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private ActivityService activityService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(activityService, "exchangeName", "fitness.exchange");
        ReflectionTestUtils.setField(activityService, "routingKey", "activity.tracking");
    }

    @Test
    void createActivity_savesAndReturnsResponse() {
        // Arrange
        ActivityRequest request = new ActivityRequest();
        request.setUserId("user-123");
        request.setType(ActivityType.RUNNING);
        request.setDuration(45);
        request.setCaloriesBurned(350);
        request.setStartTime(LocalDateTime.of(2026, 6, 1, 8, 0));
        request.setAdditionalMetrics(Map.of("distance", 6.5));

        Activity savedActivity = new Activity();
        savedActivity.setId("activity-456");
        savedActivity.setUserId(request.getUserId());
        savedActivity.setType(request.getType());
        savedActivity.setDuration(request.getDuration());
        savedActivity.setCaloriesBurned(request.getCaloriesBurned());
        savedActivity.setStartTime(request.getStartTime());
        savedActivity.setAdditionalMetrics(request.getAdditionalMetrics());
        savedActivity.setCreatedAt(LocalDateTime.now());
        savedActivity.setUpdatedAt(LocalDateTime.now());

        when(userValidationService.validateUser("user-123")).thenReturn(true);
        when(activityRepository.save(any(Activity.class))).thenReturn(savedActivity);

        // Act
        ActivityResponse response = activityService.createActivity(request);

        // Assert
        assertNotNull(response);
        assertEquals("activity-456", response.getId());
        assertEquals("user-123", response.getUserId());
        assertEquals(ActivityType.RUNNING, response.getType());
        assertEquals(45, response.getDuration());
        assertEquals(350, response.getCaloriesBurned());
        verify(userValidationService, times(1)).validateUser("user-123");
        verify(activityRepository, times(1)).save(any(Activity.class));
        verify(rabbitTemplate, times(1)).convertAndSend(eq("fitness.exchange"), eq("activity.tracking"), any(ActivityResponse.class));
    }

    @Test
    void createActivity_whenUserDoesNotExist_throwsRuntimeException() {
        // Arrange
        ActivityRequest request = new ActivityRequest();
        request.setUserId("invalid-user");

        when(userValidationService.validateUser("invalid-user")).thenReturn(false);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            activityService.createActivity(request);
        });

        assertEquals("User not found with id: invalid-user", exception.getMessage());
        verify(userValidationService, times(1)).validateUser("invalid-user");
        verify(activityRepository, never()).save(any(Activity.class));
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void getActivity_whenExists_returnsResponse() {
        // Arrange
        String activityId = "activity-456";
        Activity activity = new Activity();
        activity.setId(activityId);
        activity.setUserId("user-123");
        activity.setType(ActivityType.WALKING);
        activity.setDuration(30);

        when(activityRepository.findById(activityId)).thenReturn(Optional.of(activity));

        // Act
        ActivityResponse response = activityService.getActivity(activityId);

        // Assert
        assertNotNull(response);
        assertEquals(activityId, response.getId());
        assertEquals(ActivityType.WALKING, response.getType());
        verify(activityRepository, times(1)).findById(activityId);
    }

    @Test
    void getActivity_whenDoesNotExist_throwsNotFoundException() {
        // Arrange
        String activityId = "unknown-id";
        when(activityRepository.findById(activityId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ActivityNotFoundException.class, () -> {
            activityService.getActivity(activityId);
        });
        verify(activityRepository, times(1)).findById(activityId);
    }

    @Test
    void getUserActivities_whenExist_returnsList() {
        // Arrange
        String userId = "user-123";
        Activity activity = new Activity();
        activity.setId("act-1");
        activity.setUserId(userId);
        activity.setType(ActivityType.CARDIO);

        when(activityRepository.findByUserId(userId)).thenReturn(List.of(activity));

        // Act
        List<ActivityResponse> responses = activityService.getUserActivities(userId);

        // Assert
        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals("act-1", responses.get(0).getId());
        verify(activityRepository, times(1)).findByUserId(userId);
    }

    @Test
    void getUserActivities_whenNone_returnsEmptyList() {
        // Arrange
        String userId = "user-empty";
        when(activityRepository.findByUserId(userId)).thenReturn(Collections.emptyList());

        // Act
        List<ActivityResponse> responses = activityService.getUserActivities(userId);

        // Assert
        assertNotNull(responses);
        assertTrue(responses.isEmpty());
        verify(activityRepository, times(1)).findByUserId(userId);
    }

    @Test
    void deleteActivity_whenExists_deletesSuccessfully() {
        // Arrange
        String activityId = "act-1";
        when(activityRepository.existsById(activityId)).thenReturn(true);
        doNothing().when(activityRepository).deleteById(activityId);

        // Act & Assert
        assertDoesNotThrow(() -> {
            activityService.deleteActivity(activityId);
        });
        verify(activityRepository, times(1)).existsById(activityId);
        verify(activityRepository, times(1)).deleteById(activityId);
    }

    @Test
    void deleteActivity_whenDoesNotExist_throwsNotFoundException() {
        // Arrange
        String activityId = "act-missing";
        when(activityRepository.existsById(activityId)).thenReturn(false);

        // Act & Assert
        assertThrows(ActivityNotFoundException.class, () -> {
            activityService.deleteActivity(activityId);
        });
        verify(activityRepository, times(1)).existsById(activityId);
        verify(activityRepository, never()).deleteById(anyString());
    }
}
