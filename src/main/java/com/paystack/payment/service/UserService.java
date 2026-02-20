package com.paystack.payment.service;

import com.paystack.payment.dto.auth.AuthResponse;
import com.paystack.payment.dto.auth.LoginRequest;
import com.paystack.payment.dto.auth.RegisterRequest;
import com.paystack.payment.model.User;
import com.paystack.payment.model.UserRole;
import com.paystack.payment.repository.UserRepository;
import com.paystack.payment.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Register a new user
     */
    public Mono<AuthResponse> registerUser(RegisterRequest request) {
        log.info("Attempting to register user with email: {}", request.getEmail());

        return userRepository.findByEmail(request.getEmail())
                .flatMap(existingUser -> {
                    log.warn("User already exists with email: {}", request.getEmail());
                    return Mono.<User>error(new RuntimeException("Email already exists"));
                })
                .switchIfEmpty(Mono.defer(() -> {
                    User newUser = User.builder()
                            .email(request.getEmail())
                            .passwordHash(passwordEncoder.encode(request.getPassword()))
                            .firstName(request.getFirstName())
                            .lastName(request.getLastName())
                            .phone(request.getPhone())
                            .role(UserRole.CUSTOMER)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();

                    return userRepository.save(newUser);
                }))
                .map(user -> {
                    log.info("User registered successfully: {}", user.getEmail());

                    String accessToken = jwtTokenProvider.generateAccessToken(
                            user.getId(), user.getEmail(), user.getRole());
                    String refreshToken = jwtTokenProvider.generateRefreshToken(
                            user.getId(), user.getEmail());

                    return new AuthResponse(
                            accessToken,
                            refreshToken,
                            user.getId(),
                            user.getEmail(),
                            user.getFirstName(),
                            user.getLastName(),
                            user.getRole().name()
                    );
                });
    }

    /**
     * Login user
     */
    public Mono<AuthResponse> loginUser(LoginRequest request) {
        log.info("Attempting to login user with email: {}", request.getEmail());

        return userRepository.findByEmail(request.getEmail())
                .switchIfEmpty(Mono.error(new RuntimeException("Invalid email or password")))
                .flatMap(user -> {
                    if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
                        log.warn("Invalid password for user: {}", request.getEmail());
                        return Mono.error(new RuntimeException("Invalid email or password"));
                    }

                    log.info("User logged in successfully: {}", user.getEmail());

                    String accessToken = jwtTokenProvider.generateAccessToken(
                            user.getId(), user.getEmail(), user.getRole());
                    String refreshToken = jwtTokenProvider.generateRefreshToken(
                            user.getId(), user.getEmail());

                    return Mono.just(new AuthResponse(
                            accessToken,
                            refreshToken,
                            user.getId(),
                            user.getEmail(),
                            user.getFirstName(),
                            user.getLastName(),
                            user.getRole().name()
                    ));
                });
    }

    /**
     * Refresh access token
     */
    public Mono<AuthResponse> refreshToken(String refreshToken) {
        log.info("Attempting to refresh token");

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            return Mono.error(new RuntimeException("Invalid refresh token"));
        }

        String email = jwtTokenProvider.getEmailFromToken(refreshToken);
        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);

        return userRepository.findByEmail(email)
                .switchIfEmpty(Mono.error(new RuntimeException("User not found")))
                .map(user -> {
                    String newAccessToken = jwtTokenProvider.generateAccessToken(
                            user.getId(), user.getEmail(), user.getRole());
                    String newRefreshToken = jwtTokenProvider.generateRefreshToken(
                            user.getId(), user.getEmail());

                    return new AuthResponse(
                            newAccessToken,
                            newRefreshToken,
                            user.getId(),
                            user.getEmail(),
                            user.getFirstName(),
                            user.getLastName(),
                            user.getRole().name()
                    );
                });
    }

    /**
     * Get user by ID
     */
    public Mono<User> getUserById(Long userId) {
        return userRepository.findById(userId);
    }

    /**
     * Get user by email
     */
    public Mono<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }
}