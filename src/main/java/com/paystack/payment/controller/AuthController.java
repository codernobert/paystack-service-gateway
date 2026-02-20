package com.paystack.payment.controller;


import com.paystack.payment.dto.ApiResponse;
import com.paystack.payment.dto.auth.*;
import com.paystack.payment.security.UserPrincipal;
import com.paystack.payment.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    /**
     * Register a new user
     * POST /api/auth/register
     */
    @PostMapping("/register")
    public Mono<ResponseEntity<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registration request received for email: {}", request.getEmail());

        return userService.registerUser(request)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response))
                .onErrorResume(e -> {
                    log.error("Registration failed: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).build());
                });
    }

    /**
     * Login user
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public Mono<ResponseEntity<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login request received for email: {}", request.getEmail());

        return userService.loginUser(request)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("Login failed: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
                });
    }

    /**
     * Refresh access token
     * POST /api/auth/refresh
     */
    @PostMapping("/refresh")
    public Mono<ResponseEntity<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("Token refresh request received");

        return userService.refreshToken(request.getRefreshToken())
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("Token refresh failed: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
                });
    }

    /**
     * Get current user profile
     * GET /api/auth/me
     */
    @GetMapping("/me")
    public Mono<ResponseEntity<UserProfileResponse>> getCurrentUser(
            @AuthenticationPrincipal Mono<UserPrincipal> principalMono) {

        return principalMono
                .flatMap(principal -> userService.getUserById(principal.getUserId()))
                .map(user -> ResponseEntity.ok(UserProfileResponse.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .phone(user.getPhone())
                        .role(user.getRole().name())
                        .build()))
                .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()));
    }

    /**
     * Logout (client-side token removal, this is just for consistency)
     * POST /api/auth/logout
     */
    @PostMapping("/logout")
    public Mono<ResponseEntity<ApiResponse>> logout() {
        log.info("Logout request received");

        return Mono.just(ResponseEntity.ok(
                ApiResponse.builder()
                        .success(true)
                        .message("Logged out successfully. Please remove tokens from client.")
                        .build()
        ));
    }
}