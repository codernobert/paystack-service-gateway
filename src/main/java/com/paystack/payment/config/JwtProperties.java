package com.paystack.payment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String secret;
    private Long expiration; // Access token expiration in milliseconds (default: 24 hours)
    private Long refreshExpiration; // Refresh token expiration in milliseconds (default: 7 days)
}
