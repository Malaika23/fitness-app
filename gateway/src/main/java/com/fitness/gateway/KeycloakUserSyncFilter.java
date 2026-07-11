package com.fitness.gateway;

import com.fitness.gateway.user.RegisterRequest;
import com.fitness.gateway.user.UserService;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Slf4j
@RequiredArgsConstructor
public class KeycloakUserSyncFilter implements WebFilter {
    private final UserService userService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String token = exchange.getRequest().getHeaders().getFirst("Authorization");
        String userId = exchange.getRequest().getHeaders().getFirst("X-User-ID");
        
        log.info("KeycloakUserSyncFilter - incoming request path: {}, Authorization header present: {}, X-User-ID: {}", 
                 exchange.getRequest().getPath(), token != null, userId);

        RegisterRequest registerRequest = getUserDetails(token);
        log.info("KeycloakUserSyncFilter - extracted registerRequest: {}", registerRequest);

        if (userId == null && registerRequest != null) {
            userId = registerRequest.getKeycloakId();
            log.info("KeycloakUserSyncFilter - fallback userId from token keycloakId: {}", userId);
        }

        if (userId != null && token != null) {
            String finalUserId = userId;
            log.info("KeycloakUserSyncFilter - executing validation check for userId: {}", userId);
            return userService.validateUser(userId)
                    .flatMap(exist -> {
                        log.info("KeycloakUserSyncFilter - validateUser exist result: {}", exist);
                        if (!exist) {
                            if (registerRequest != null) {
                                log.info("KeycloakUserSyncFilter - user does not exist. Triggering registerUser...");
                                return userService.registerUser(registerRequest)
                                        .then(Mono.empty());
                            } else {
                                log.warn("KeycloakUserSyncFilter - user does not exist but registerRequest is null. Skipping registration.");
                                return Mono.empty();
                            }
                        } else {
                            log.info("User already exist, Skipping sync.");
                            return Mono.empty();
                        }
                    })
                    .then(Mono.defer(() -> {
                        log.info("KeycloakUserSyncFilter - mutating request header with X-User-ID: {}", finalUserId);
                        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                                .header("X-User-ID", finalUserId)
                                .build();
                        return chain.filter(exchange.mutate().request(mutatedRequest).build());
                    }));
        }
        log.info("KeycloakUserSyncFilter - skipping sync execution and proceeding downstream");
        return chain.filter(exchange);
    }

    private RegisterRequest getUserDetails(String token) {
        if (token == null) {
            log.info("getUserDetails - token is null");
            return null;
        }
        try {
            String tokenWithoutBearer = token.replace("Bearer ", "").trim();
            SignedJWT signedJWT = SignedJWT.parse(tokenWithoutBearer);
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

            RegisterRequest registerRequest = new RegisterRequest();
            registerRequest.setEmail(claims.getStringClaim("email"));
            registerRequest.setKeycloakId(claims.getStringClaim("sub"));
            registerRequest.setPassword("dummy@123123");
            registerRequest.setFirstName(claims.getStringClaim("given_name"));
            registerRequest.setLastName(claims.getStringClaim("family_name"));
            return registerRequest;
        } catch (Exception e) {
            log.error("getUserDetails - failed to parse token: {}", e.getMessage(), e);
            return null;
        }
    }
}