package com.paystack.payment.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements WebFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String token = extractTokenFromRequest(exchange.getRequest());

        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
            return authenticateWithToken(token, exchange, chain);
        }

        return chain.filter(exchange);
    }

    /**
     * Extract JWT token from Authorization header
     */
    private String extractTokenFromRequest(ServerHttpRequest request) {
        String bearerToken = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        return null;
    }

    /**
     * Authenticate user with token
     */
    private Mono<Void> authenticateWithToken(String token, ServerWebExchange exchange, WebFilterChain chain) {
        try {
            String email = jwtTokenProvider.getEmailFromToken(token);
            Long userId = jwtTokenProvider.getUserIdFromToken(token);
            var role = jwtTokenProvider.getRoleFromToken(token);

            // Create authentication object
            var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
            var authentication = new UsernamePasswordAuthenticationToken(
                    new UserPrincipal(userId, email, role),
                    null,
                    authorities
            );

            // Set authentication in context and continue filter chain
            return chain.filter(exchange)
                    .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));

        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage());
            return chain.filter(exchange);
        }
    }
}
