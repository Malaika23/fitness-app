package com.fitness.activityservice.controller;

import com.fitness.activityservice.exception.ActivityNotFoundException;
import com.fitness.activityservice.model.ActivityType;
import com.fitness.activityservice.service.ActivityService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ActivityController.class)
class ActivityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ActivityService activityService;

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
                LocalDateTime.parse("2026-02-12T11:00:00")
        );

        when(activityService.getActivity("activity-1")).thenReturn(response);

        mockMvc.perform(get("/api/activities/activity-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("activity-1"))
                .andExpect(jsonPath("$.userId").value("user-1"))
                .andExpect(jsonPath("$.type").value("RUNNING"));
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
