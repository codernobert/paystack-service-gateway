package com.paystack.payment.config;

import com.paystack.payment.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .authorizeExchange(exchanges -> exchanges
                        // Actuator endpoints - Public (must be before static resources)
                        .pathMatchers("/actuator", "/actuator/**").permitAll()
                        .pathMatchers("/health").permitAll()

                        // Public endpoints - Authentication
                        .pathMatchers("/api/auth/**").permitAll()

                        // Public endpoints - Products (read-only)
                        .pathMatchers(HttpMethod.GET, "/api/products/**").permitAll()

                        // Public endpoints - Dashboard & static resources
                        // Note: These patterns are for static files in root/public/static folders ONLY
                        .pathMatchers("/*.html").permitAll()
                        .pathMatchers("/*.css").permitAll()
                        .pathMatchers("/*.js").permitAll()
                        .pathMatchers("/*.ico").permitAll()
                        .pathMatchers("/static/**").permitAll()
                        .pathMatchers("/public/**").permitAll()

                        // Admin endpoints - require admin role
                        .pathMatchers("/api/admin/**").hasRole("ADMIN")

                        // Protected endpoints - require authentication
                        .pathMatchers("/api/cart/**").authenticated()
                        .pathMatchers("/api/checkout/**").authenticated()
                        .pathMatchers("/api/orders/**").authenticated()

                        // All other requests require authentication
                        .anyExchange().authenticated()
                )
                .addFilterAt(jwtAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
