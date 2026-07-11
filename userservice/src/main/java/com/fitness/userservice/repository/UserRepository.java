package com.fitness.userservice.repository;

import com.fitness.userservice.model.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {

    boolean existsByEmail(
            @NotBlank(message = "Email is required") @Email(message = "Invalid email format") String email);

    Boolean existsByKeycloakId(String userId);

    Optional<User> findByKeycloakId(String keycloakId);

    User findByEmail(String email);
}
