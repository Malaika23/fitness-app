package com.fitness.userservice.service;

import com.fitness.userservice.controller.RegisterRequest;
import com.fitness.userservice.controller.UserResponse;
import com.fitness.userservice.model.User;
import com.fitness.userservice.model.UserRole;
import com.fitness.userservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void getUserProfile_whenUserExists_returnsUserResponse() {
        // Arrange
        String userId = "test-uuid";
        User user = new User();
        user.setId(userId);
        user.setEmail("john.doe@example.com");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setRole(UserRole.USER);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act
        UserResponse response = userService.getUserProfile(userId);

        // Assert
        assertNotNull(response);
        assertEquals(userId, response.getId());
        assertEquals("john.doe@example.com", response.getEmail());
        assertEquals("John", response.getFirstName());
        assertEquals("Doe", response.getLastName());
        assertEquals(UserRole.USER, response.getRole());
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    void getUserProfile_whenUserDoesNotExist_throwsRuntimeException() {
        // Arrange
        String userId = "unknown-uuid";
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.getUserProfile(userId);
        });

        assertEquals("User not found with id: " + userId, exception.getMessage());
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    void register_whenNewEmail_savesAndReturnsResponse() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setEmail("new.user@example.com");
        request.setPassword("securePassword");
        request.setFirstName("Jane");
        request.setLastName("Smith");

        User savedUser = new User();
        savedUser.setId("new-uuid");
        savedUser.setEmail(request.getEmail());
        savedUser.setPassword(request.getPassword());
        savedUser.setFirstName(request.getFirstName());
        savedUser.setLastName(request.getLastName());
        savedUser.setRole(UserRole.USER);
        savedUser.setCreatedAt(LocalDateTime.now());
        savedUser.setUpdatedAt(LocalDateTime.now());

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Act
        UserResponse response = userService.register(request);

        // Assert
        assertNotNull(response);
        assertEquals("new-uuid", response.getId());
        assertEquals("new.user@example.com", response.getEmail());
        assertEquals("Jane", response.getFirstName());
        assertEquals("Smith", response.getLastName());
        verify(userRepository, times(1)).existsByEmail(request.getEmail());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void register_whenEmailAlreadyExists_throwsRuntimeException() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setEmail("existing@example.com");
        request.setPassword("password123");

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.register(request);
        });

        assertEquals("Email already exist", exception.getMessage());
        verify(userRepository, times(1)).existsByEmail(request.getEmail());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void existByUserId_whenUserExists_returnsTrue() {
        // Arrange
        String userId = "test-uuid";
        when(userRepository.existsById(userId)).thenReturn(true);

        // Act
        Boolean exists = userService.existByUserId(userId);

        // Assert
        assertTrue(exists);
        verify(userRepository, times(1)).existsById(userId);
    }

    @Test
    void existByUserId_whenUserDoesNotExist_returnsFalse() {
        // Arrange
        String userId = "unknown-uuid";
        when(userRepository.existsById(userId)).thenReturn(false);

        // Act
        Boolean exists = userService.existByUserId(userId);

        // Assert
        assertFalse(exists);
        verify(userRepository, times(1)).existsById(userId);
    }
}
