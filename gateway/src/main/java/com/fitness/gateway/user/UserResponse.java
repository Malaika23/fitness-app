package com.fitness.gateway.user;

import lombok.Data;

@Data
public class UserResponse {
    private String id;
    private String keycloakId;
    private String email;
    private String firstName;
    private String lastName;
}
