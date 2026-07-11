package com.fitness.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitness.userservice.model.UserRole;
import com.fitness.userservice.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setValidator(validator)
                .build();
    }

    @Test
    void getUserProfile_whenUserExists_returns200AndUserResponse() throws Exception {
        // Arrange
        String userId = "user-123";
        UserResponse response = new UserResponse(
                userId,
                null,
                "john.doe@example.com",
                "John",
                "Doe",
                UserRole.USER,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        when(userService.getUserProfile(userId)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/user/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.role").value("USER"));

        verify(userService, times(1)).getUserProfile(userId);
    }

    @Test
    void register_withValidData_returns200AndUserResponse() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setEmail("jane.smith@fitness.com");
        request.setPassword("securePassword123");
        request.setFirstName("Jane");
        request.setLastName("Smith");

        UserResponse response = new UserResponse(
                "new-uuid",
                null,
                "jane.smith@fitness.com",
                "Jane",
                "Smith",
                UserRole.USER,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        when(userService.register(any(RegisterRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("new-uuid"))
                .andExpect(jsonPath("$.email").value("jane.smith@fitness.com"))
                .andExpect(jsonPath("$.firstName").value("Jane"))
                .andExpect(jsonPath("$.lastName").value("Smith"));

        verify(userService, times(1)).register(any(RegisterRequest.class));
    }

    @Test
    void register_withBlankEmail_returns400BadRequest() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setEmail(""); // Blank
        request.setPassword("securePassword123");
        request.setFirstName("Jane");
        request.setLastName("Smith");

        // Act & Assert
        mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).register(any(RegisterRequest.class));
    }

    @Test
    void register_withInvalidEmailFormat_returns400BadRequest() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setEmail("invalid-email-format"); // Invalid
        request.setPassword("securePassword123");
        request.setFirstName("Jane");
        request.setLastName("Smith");

        // Act & Assert
        mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).register(any(RegisterRequest.class));
    }

    @Test
    void register_withShortPassword_returns400BadRequest() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setEmail("jane.smith@fitness.com");
        request.setPassword("12345"); // too short (size < 6)
        request.setFirstName("Jane");
        request.setLastName("Smith");

        // Act & Assert
        mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).register(any(RegisterRequest.class));
    }

    @Test
    void validateUser_whenExists_returnsTrue() throws Exception {
        // Arrange
        String userId = "existing-id";
        when(userService.existByUserId(userId)).thenReturn(true);

        // Act & Assert
        mockMvc.perform(get("/api/user/{userId}/validate", userId))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(userService, times(1)).existByUserId(userId);
    }

    @Test
    void validateUser_whenDoesNotExist_returnsFalse() throws Exception {
        // Arrange
        String userId = "missing-id";
        when(userService.existByUserId(userId)).thenReturn(false);

        // Act & Assert
        mockMvc.perform(get("/api/user/{userId}/validate", userId))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));

        verify(userService, times(1)).existByUserId(userId);
    }
}
