package com.paystack.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

