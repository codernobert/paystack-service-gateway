package com.paystack.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentIntentResponse {
    private String clientSecret; // Access code for Paystack
    private String paymentIntentId; // Transaction reference
    private String status;
    private String authorizationUrl; // URL to redirect user for payment (Paystack specific)
}
