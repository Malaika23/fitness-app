package com.fitness.activityservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class ActivityServiceApplication {

    private static void configureTimeZone() {
        System.setProperty("user.timezone", "UTC");
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    public static void main(String[] args) {
        configureTimeZone();
        SpringApplication.run(ActivityServiceApplication.class, args);
    }
}
