package com.fitness.activityservice.exception;

public class ActivityNotFoundException extends RuntimeException {

    public ActivityNotFoundException(String activityId) {
        super("Activity not found with id: " + activityId);
    }
}
