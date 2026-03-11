package com.paystack.payment.service.impl;

import com.nova.auth.client.AuthServiceClient;
import com.nova.auth.client.dto.TokenGenerationResponse;
import com.nova.auth.client.dto.TokenValidationResponse;
import com.paystack.payment.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AuthServiceClient authClient;

    @Override
    public Mono<TokenGenerationResponse> generateToken(String username, String password) {
        log.debug("Generating token for user: {}", username);
        return authClient.generateToken(username, password)
                .doOnNext(r -> log.info("Token generated successfully for userId={}, username={}",
                        r.getUserId(), r.getUsername()))
                .onErrorResume(e -> {
                    log.error("Token generation failed for user {}: {}", username, e.getMessage());
                    return Mono.empty();
                });
    }

    @Override
    public Mono<TokenValidationResponse> validateToken(String token) {
        log.debug("Validating token");
        return authClient.validateToken(token)
                .doOnNext(r -> log.debug("Token validation result: valid={}, user={}", r.isValid(), r.getUsername()))
                .onErrorResume(e -> {
                    log.error("Token validation error: {}", e.getMessage());
                    return Mono.just(TokenValidationResponse.builder()
                            .valid(false)
                            .reason("Auth service unavailable: " + e.getMessage())
                            .build());
                });
    }
}

