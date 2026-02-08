package com.paystack.payment.dto.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Internal DTO for Paystack Verify Transaction API response.
 * Package-private to hide Paystack implementation details.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaystackVerifyResponse {
    private boolean status;
    private String message;
    private PaystackVerificationData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaystackVerificationData {
        private Long id;
        private String domain;
        private String status; // "success", "failed", etc.
        private String reference;
        private Long amount; // Amount in kobo
        private String message;

        @JsonProperty("gateway_response")
        private String gatewayResponse;

        @JsonProperty("paid_at")
        private String paidAt;

        @JsonProperty("created_at")
        private String createdAt;

        private String channel; // "card", "mobile_money", etc.
        private String currency;

        @JsonProperty("ip_address")
        private String ipAddress;

        private Customer customer;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Customer {
        private Long id;
        private String email;

        @JsonProperty("customer_code")
        private String customerCode;
    }
}

