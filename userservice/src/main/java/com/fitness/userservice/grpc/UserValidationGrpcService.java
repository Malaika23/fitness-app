package com.fitness.userservice.grpc;

import com.fitness.userservice.service.UserService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class UserValidationGrpcService extends UserValidationServiceGrpc.UserValidationServiceImplBase {

    private final UserService userService;

    @Override
    public void validateUser(UserValidationRequest request, StreamObserver<UserValidationResponse> responseObserver) {
        String userId = request.getUserId();
        log.info("gRPC call: validating user ID: {}", userId);
        boolean exists = false;
        try {
            exists = userService.existByUserId(userId);
        } catch (Exception e) {
            log.error("Error validating user ID {} via gRPC", userId, e);
        }
        UserValidationResponse response = UserValidationResponse.newBuilder()
                .setExists(exists)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
