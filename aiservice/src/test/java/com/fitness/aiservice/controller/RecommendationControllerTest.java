package com.fitness.aiservice.controller;

import com.fitness.aiservice.model.Recommendation;
import com.fitness.aiservice.service.RecommendationService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RecommendationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private RecommendationService recommendationService;

    @InjectMocks
    private RecommendationController recommendationController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(recommendationController)
                .setControllerAdvice(new TestExceptionHandler())
                .build();
    }

    @RestControllerAdvice
    static class TestExceptionHandler {
        @ExceptionHandler(RuntimeException.class)
        public ResponseEntity<String> handleRuntime(RuntimeException ex) {
            return ResponseEntity.internalServerError().body(ex.getMessage());
        }
    }

    @Test
    void getUserRecommendation_returnsList() throws Exception {
        Recommendation rec = new Recommendation();
        rec.setId("rec1");
        rec.setUserId("user1");
        rec.setRecommendation("Maintain a proper stance.");

        when(recommendationService.getUserRecommendations("user1")).thenReturn(List.of(rec));

        mockMvc.perform(get("/api/recommendation/user/user1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("rec1"))
                .andExpect(jsonPath("$[0].userId").value("user1"))
                .andExpect(jsonPath("$[0].recommendation").value("Maintain a proper stance."));
    }

    @Test
    void getUserRecommendation_whenEmpty_returnsEmptyList() throws Exception {
        when(recommendationService.getUserRecommendations("user-empty")).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/recommendation/user/user-empty"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getActivityRecommendation_returnsRecommendation() throws Exception {
        Recommendation rec = new Recommendation();
        rec.setId("rec1");
        rec.setActivityId("act1");
        rec.setRecommendation("Maintain a proper stance.");

        when(recommendationService.getActivityRecommendations("act1")).thenReturn(rec);

        mockMvc.perform(get("/api/recommendation/activity/act1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("rec1"))
                .andExpect(jsonPath("$.activityId").value("act1"))
                .andExpect(jsonPath("$.recommendation").value("Maintain a proper stance."));
    }

    @Test
    void getActivityRecommendation_whenNotFound_returnsInternalServerError() throws Exception {
        when(recommendationService.getActivityRecommendations("missing-act"))
                .thenThrow(new RuntimeException("Recommendation Not Found for activity Id missing-act"));

        mockMvc.perform(get("/api/recommendation/activity/missing-act"))
                .andExpect(status().isInternalServerError());
    }
}
