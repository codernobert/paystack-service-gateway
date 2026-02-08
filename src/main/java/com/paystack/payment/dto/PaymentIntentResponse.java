package com.paystack.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Public API response DTO for payment initialization.
 * This is the main DTO that consuming services receive after initiating payment.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentIntentResponse {
    private String clientSecret; // Access code for Paystack
    private String paymentIntentId; // Transaction reference
    private String status; // Payment status (e.g., "initialized")
    private String authorizationUrl; // URL to redirect user for payment
}

