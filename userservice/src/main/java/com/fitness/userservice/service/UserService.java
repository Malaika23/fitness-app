package com.fitness.userservice.service;

import com.fitness.userservice.controller.RegisterRequest;
import com.fitness.userservice.controller.UserResponse;
import com.fitness.userservice.model.User;
import com.fitness.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    public UserResponse getUserProfile(String userId) {
        User user = userRepository.findByKeycloakId(userId)
                .orElseGet(() -> userRepository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("User not found with id: " + userId)));

        return toResponse(user);
    }

    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            User existingUser = userRepository.findByEmail(request.getEmail());
            if (existingUser.getKeycloakId() != null && !existingUser.getKeycloakId().equals(request.getKeycloakId())) {
                throw new RuntimeException("Email already in use by another account: " + request.getEmail());
            }
            if (existingUser.getKeycloakId() == null) {
                existingUser.setKeycloakId(request.getKeycloakId());
                existingUser = userRepository.save(existingUser);
            }
            return toResponse(existingUser);
        }
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setKeycloakId(request.getKeycloakId());

        return toResponse(userRepository.save(user));
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getKeycloakId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }

    public Boolean existByUserId(String userId) {
        log.info("Calling User Validation API for userId: {}", userId);
        return userRepository.existsByKeycloakId(userId) || userRepository.existsById(userId);
    }
}
