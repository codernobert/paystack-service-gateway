package com.paystack.payment.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the Payment Service Client.
 *
 * Used by other services to configure the WebClient for calling the payment service.
 *
 * Properties in application.properties or application.yml:
 *
 * payment.service.url=http://payment-service:8080
 * payment.service.connect-timeout=5000
 * payment.service.read-timeout=10000
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "payment.service")
public class PaymentServiceClientProperties {
    private String url = "http://localhost:8080";
    private int connectTimeout = 5000; // milliseconds
    private int readTimeout = 10000;   // milliseconds
    private int writeTimeout = 10000;  // milliseconds
}

