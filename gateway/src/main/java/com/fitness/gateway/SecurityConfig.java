package com.fitness.gateway;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.cors.CorsConfiguration;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.disable()) // Let CorsWebFilter handle CORS
                .authorizeExchange(auth -> auth
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll() // Permit all CORS preflight requests
                        .pathMatchers("/actuator/**", "/api/user/register").permitAll()
                        .pathMatchers("/api/activities/user/{userId}").access((authentication, context) -> {
                            String userId = context.getVariables().get("userId").toString();
                            return authentication.map(a -> {
                                if (a.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt jwt) {
                                    return new org.springframework.security.authorization.AuthorizationDecision(jwt.getSubject().equals(userId));
                                }
                                return new org.springframework.security.authorization.AuthorizationDecision(false);
                            });
                        })
                        .pathMatchers("/api/recommendation/user/{userId}").access((authentication, context) -> {
                            String userId = context.getVariables().get("userId").toString();
                            return authentication.map(a -> {
                                if (a.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt jwt) {
                                    return new org.springframework.security.authorization.AuthorizationDecision(jwt.getSubject().equals(userId));
                                }
                                return new org.springframework.security.authorization.AuthorizationDecision(false);
                            });
                        })
                        .anyExchange().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .build();
    }

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.addAllowedOrigin("http://localhost:5173");
        corsConfig.addAllowedMethod(HttpMethod.GET);
        corsConfig.addAllowedMethod(HttpMethod.POST);
        corsConfig.addAllowedMethod(HttpMethod.PUT);
        corsConfig.addAllowedMethod(HttpMethod.DELETE);
        corsConfig.addAllowedMethod(HttpMethod.OPTIONS);
        corsConfig.addAllowedHeader("Authorization");
        corsConfig.addAllowedHeader("Content-Type");
        corsConfig.addAllowedHeader("X-USER-ID");
        corsConfig.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", corsConfig);

        return new CorsWebFilter(source);
    }
}
