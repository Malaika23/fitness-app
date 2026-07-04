package com.fitness.aiservice.service;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import com.fitness.aiservice.model.Recommendation;
import com.fitness.aiservice.repository.RecommendationRepository;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final RecommendationRepository recommendationRepository;

    public void saveRecommendation(Recommendation recommendation) {
        recommendationRepository.save(recommendation);
    }

    public List<Recommendation> getUserRecommendations(String userId) {
        return recommendationRepository.findByUserId(userId);
    }

    public Recommendation getActivityRecommendations(String activityId) {
        return recommendationRepository.findByActivityId(activityId)
                .orElseThrow(() -> new RuntimeException("Recommendation Not Found for activity Id " + activityId));
    }

}
