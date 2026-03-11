package com.paystack.payment.service;

import com.nova.auth.client.dto.TokenGenerationResponse;
import com.nova.auth.client.dto.TokenValidationResponse;
import reactor.core.publisher.Mono;

public interface AuthService {

    /**
     * Authenticate with the nova-auth-service using username/password and return a token.
     * Emits empty if credentials are invalid.
     */
    Mono<TokenGenerationResponse> generateToken(String username, String password);

    /**
     * Validate a Bearer token against nova-auth-service.
     * Always emits — returns false if the token is invalid or auth-service is unreachable.
     */
    Mono<TokenValidationResponse> validateToken(String token);
}

