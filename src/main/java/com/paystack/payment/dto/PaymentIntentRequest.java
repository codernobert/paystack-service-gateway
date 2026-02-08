package com.paystack.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Public API request DTO for initiating Paystack payment.
 * This is the main DTO that consuming services should use.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentIntentRequest {
    @NotNull(message = "Amount is required")
    private Long amount; // Amount in smallest currency unit (kobo for NGN, cents for KES, etc.)

    @NotBlank(message = "Currency is required")
    private String currency; // e.g., "NGN", "KES", "USD"

    private String description; // Description of the payment

    private String email; // Customer email (required for Paystack)

    private String callbackUrl; // URL to redirect after payment (optional, falls back to configured URL)
}

