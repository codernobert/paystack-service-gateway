package com.paystack.payment.dto.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Internal DTO for Paystack Initialize Transaction API request.
 * Package-private to hide Paystack implementation details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaystackInitializeRequest {
    private Long amount; // Amount in kobo (smallest currency unit)
    private String email;
    private String currency; // e.g., "KES" for Kenya Shillings
    private String reference; // Unique transaction reference

    @JsonProperty("callback_url")
    private String callbackUrl;

    private List<String> channels; // Payment channels: ["card", "bank", "ussd", "mobile_money"]
}

