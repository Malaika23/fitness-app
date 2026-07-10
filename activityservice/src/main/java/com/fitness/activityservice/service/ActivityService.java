package com.fitness.activityservice.service;

import com.fitness.activityservice.controller.ActivityRequest;
import com.fitness.activityservice.controller.ActivityResponse;
import com.fitness.activityservice.exception.ActivityNotFoundException;
import com.fitness.activityservice.model.Activity;
import com.fitness.activityservice.repository.ActivityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final UserValidationService userValidationService;
    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.name}")
    private String exchangeName;

    @Value("${rabbitmq.routing.key}")
    private String routingKey;

    @Value("${rabbitmq.queue.name}")
    private String queueName;

    public ActivityResponse createActivity(ActivityRequest request) {
        boolean userExists = userValidationService.validateUser(request.getUserId());
        if (!userExists) {
            throw new RuntimeException("User not found with id: " + request.getUserId());
        }

        LocalDateTime now = LocalDateTime.now();

        Activity activity = new Activity();
        activity.setUserId(request.getUserId());
        activity.setType(request.getType());
        activity.setDuration(request.getDuration());
        activity.setCaloriesBurned(request.getCaloriesBurned());
        activity.setStartTime(request.getStartTime());
        activity.setAdditionalMetrics(request.getAdditionalMetrics());
        activity.setCreatedAt(now);
        activity.setUpdatedAt(now);

        ActivityResponse response = toResponse(activityRepository.save(activity));

        // Publish to RabbitMQ for AI Processing
        try {
            rabbitTemplate.convertAndSend(exchangeName, routingKey, response);
        } catch (Exception e) {
            log.error("Failed to send activity to RabbitMQ", e);
        }

        return response;
    }

    public ActivityResponse getActivity(String activityId) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ActivityNotFoundException(activityId));

        return toResponse(activity);
    }

    public List<ActivityResponse> getUserActivities(String userId) {
        return activityRepository.findByUserId(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public void deleteActivity(String activityId) {
        if (!activityRepository.existsById(activityId)) {
            throw new ActivityNotFoundException(activityId);
        }

        activityRepository.deleteById(activityId);
    }

    private ActivityResponse toResponse(Activity activity) {
        return new ActivityResponse(
                activity.getId(),
                activity.getUserId(),
                activity.getType(),
                activity.getDuration(),
                activity.getCaloriesBurned(),
                activity.getStartTime(),
                activity.getAdditionalMetrics(),
                activity.getCreatedAt(),
                activity.getUpdatedAt());
    }
}
