package com.fitness.aiservice.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import com.fitness.aiservice.model.Activity;

@Service
@Slf4j
@RequiredArgsConstructor
public class ActivityMessageListener {

    private final ActivityAiService activityAiService;

    @RabbitListener(queues = "${rabbitmq.queue.name}")
    public void processActivity(Activity activity) {
        log.info("Received activity for AI processing: {}", activity.getId());
        log.info("Generated Recommendation: {}", activityAiService.generateRecommendation(activity));
        // AI processing logic here
    }
}
