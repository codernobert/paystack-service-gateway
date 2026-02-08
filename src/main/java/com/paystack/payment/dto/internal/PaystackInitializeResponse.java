package com.paystack.payment.dto.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Internal DTO for Paystack Initialize Transaction API response.
 * Package-private to hide Paystack implementation details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaystackInitializeResponse {
    private boolean status;
    private String message;
    private PaystackData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaystackData {
        @JsonProperty("authorization_url")
        private String authorizationUrl;

        @JsonProperty("access_code")
        private String accessCode;

        private String reference;
    }
}

