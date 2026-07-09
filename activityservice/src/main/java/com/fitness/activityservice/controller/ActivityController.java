package com.fitness.activityservice.controller;

import com.fitness.activityservice.service.ActivityService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/activities")
@AllArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    @PostMapping
    public ResponseEntity<ActivityResponse> createActivity(@RequestBody ActivityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(activityService.createActivity(request));
    }

    @GetMapping("/{activityId}")
    public ResponseEntity<ActivityResponse> getActivity(@PathVariable String activityId) {
        return ResponseEntity.ok(activityService.getActivity(activityId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ActivityResponse>> getUserActivities(@PathVariable String userId) {
        return ResponseEntity.ok(activityService.getUserActivities(userId));
    }

    @DeleteMapping("/{activityId}")
    public ResponseEntity<Void> deleteActivity(@PathVariable String activityId) {
        activityService.deleteActivity(activityId);
        return ResponseEntity.noContent().build();
    }
}
