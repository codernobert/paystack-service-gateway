package com.paystack.payment.config;

import com.nova.auth.client.AuthServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Reactive web filter that validates Bearer tokens on every protected endpoint.
 * Consumers must obtain a JWT from nova-auth-service and include it as:
 *   Authorization: Bearer <token>
 *
 * Public (no-auth) paths: /api/payments/callback, /api/payments/health
 */
@Component
@Order(-100)
@RequiredArgsConstructor
@Slf4j
public class AuthWebFilter implements WebFilter {

    private final AuthServiceClient authClient;

    /** Paths that do NOT require a Bearer token (Paystack-called or infra endpoints). */
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/payments/callback",
            "/api/payments/health"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

        String path = exchange.getRequest().getPath().value();

        if (PUBLIC_PATHS.stream().anyMatch(path::startsWith)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest()
                .getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or malformed Authorization header — path={}", path);
            return unauthorized(exchange, "Missing or malformed Authorization header");
        }

        String token = authHeader.substring(7);

        return authClient.validateToken(token)
                .flatMap(validation -> {
                    if (!validation.isValid()) {
                        log.warn("Token rejected — path={} reason={}", path, validation.getReason());
                        return unauthorized(exchange, "Invalid or expired token");
                    }
                    log.debug("Token accepted — user={} path={}", validation.getUsername(), path);
                    return chain.filter(exchange);
                })
                .onErrorResume(e -> {
                    log.error("Auth service error during token validation — path={}: {}", path, e.getMessage());
                    return unauthorized(exchange, "Auth service unavailable");
                });
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String reason) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body = ("{\"status\":false,\"message\":\"" + reason + "\"}").getBytes();
        return exchange.getResponse()
                .writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(body)));
    }
}

