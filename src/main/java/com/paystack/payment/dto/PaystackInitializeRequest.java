package com.paystack.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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

    private List<String> channels; // Payment channels: ["card", "bank", "ussd", "mobile_money"] (includes MPESA)
}

