package com.fitness.activityservice.service;

import org.springframework.stereotype.Service;
import com.fitness.userservice.grpc.UserValidationServiceGrpc;
import com.fitness.userservice.grpc.UserValidationRequest;
import com.fitness.userservice.grpc.UserValidationResponse;
import net.devh.boot.grpc.client.inject.GrpcClient;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class UserValidationService {

    @GrpcClient("userservice")
    private UserValidationServiceGrpc.UserValidationServiceBlockingStub userValidationServiceBlockingStub;

    public boolean validateUser(String userId) {
        try {
            log.info("Validating user ID {} via gRPC", userId);
            UserValidationRequest request = UserValidationRequest.newBuilder()
                    .setUserId(userId)
                    .build();
            UserValidationResponse response = userValidationServiceBlockingStub.validateUser(request);
            return response.getExists();
        } catch (Exception e) {
            log.error("Error validating user ID {} via gRPC", userId, e);
            throw new RuntimeException("Error validating user", e);
        }
    }
}
