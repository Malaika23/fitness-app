package com.fitness.activityservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitness.activityservice.exception.ActivityNotFoundException;
import com.fitness.activityservice.exception.GlobalExceptionHandler;
import com.fitness.activityservice.model.ActivityType;
import com.fitness.activityservice.service.ActivityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ActivityControllerTest {

        private MockMvc mockMvc;

        @Mock
        private ActivityService activityService;

        @InjectMocks
        private ActivityController activityController;

        private final ObjectMapper objectMapper = new ObjectMapper();

        @BeforeEach
        void setUp() {
                mockMvc = MockMvcBuilders.standaloneSetup(activityController)
                                .setControllerAdvice(new GlobalExceptionHandler())
                                .build();
        }

        @Test
        void createActivity_returnsCreatedAndActivityResponse() throws Exception {
                ActivityRequest request = new ActivityRequest();
                request.setUserId("user-1");
                request.setType(ActivityType.RUNNING);
                request.setDuration(30);
                request.setCaloriesBurned(250);

                ActivityResponse response = new ActivityResponse(
                                "activity-1",
                                "user-1",
                                ActivityType.RUNNING,
                                30,
                                250,
                                LocalDateTime.parse("2026-02-12T10:00:00"),
                                Map.of(),
                                LocalDateTime.parse("2026-02-12T11:00:00"),
                                LocalDateTime.parse("2026-02-12T11:00:00"));

                when(activityService.createActivity(any(ActivityRequest.class))).thenReturn(response);

                mockMvc.perform(post("/api/activities")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").value("activity-1"))
                                .andExpect(jsonPath("$.userId").value("user-1"))
                                .andExpect(jsonPath("$.type").value("RUNNING"))
                                .andExpect(jsonPath("$.duration").value(30))
                                .andExpect(jsonPath("$.caloriesBurned").value(250));
        }

        @Test
        void getActivityReturnsSingleActivityById() throws Exception {
                ActivityResponse response = new ActivityResponse(
                                "activity-1",
                                "user-1",
                                ActivityType.RUNNING,
                                30,
                                250,
                                LocalDateTime.parse("2026-02-12T10:00:00"),
                                Map.of("distance", 5.2),
                                LocalDateTime.parse("2026-02-12T11:00:00"),
                                LocalDateTime.parse("2026-02-12T11:00:00"));

                when(activityService.getActivity("activity-1")).thenReturn(response);

                mockMvc.perform(get("/api/activities/activity-1"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value("activity-1"))
                                .andExpect(jsonPath("$.userId").value("user-1"))
                                .andExpect(jsonPath("$.type").value("RUNNING"));
        }

        @Test
        void getActivity_whenNotFound_returnsNotFound() throws Exception {
                when(activityService.getActivity("missing-id")).thenThrow(new ActivityNotFoundException("missing-id"));

                mockMvc.perform(get("/api/activities/missing-id"))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.message").value("Activity not found with id: missing-id"));
        }

        @Test
        void getUserActivities_returnsList() throws Exception {
                ActivityResponse response = new ActivityResponse(
                                "activity-1",
                                "user-1",
                                ActivityType.RUNNING,
                                30,
                                250,
                                null,
                                Map.of(),
                                null,
                                null);

                when(activityService.getUserActivities("user-1")).thenReturn(List.of(response));

                mockMvc.perform(get("/api/activities/user/user-1"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].id").value("activity-1"))
                                .andExpect(jsonPath("$[0].userId").value("user-1"));
        }

        @Test
        void getUserActivities_whenEmpty_returnsEmptyList() throws Exception {
                when(activityService.getUserActivities("user-empty")).thenReturn(Collections.emptyList());

                mockMvc.perform(get("/api/activities/user/user-empty"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        void deleteActivity_whenExists_returnsNoContent() throws Exception {
                doNothing().when(activityService).deleteActivity("activity-1");

                mockMvc.perform(delete("/api/activities/activity-1"))
                                .andExpect(status().isNoContent());
        }

        @Test
        void deleteMissingActivityReturnsNotFound() throws Exception {
                doThrow(new ActivityNotFoundException("missing-id"))
                                .when(activityService)
                                .deleteActivity("missing-id");

                mockMvc.perform(delete("/api/activities/missing-id"))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.message").value("Activity not found with id: missing-id"));
        }
}
