package com.paystack.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentIntentRequest {


    // --- Payment details ---
    @NotNull(message = "Amount is required")
    private Long amount;

    @NotBlank(message = "Currency is required")
    private String currency;

    private String description;

    private String email; // Customer email (required for Paystack)

    private String callbackUrl; // URL to redirect after payment (optional, falls back to configured URL)
}
